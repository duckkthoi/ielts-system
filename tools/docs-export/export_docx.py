"""Xuất tài liệu Markdown trong docs/ sang Word (.docx) vào thư mục docx/.

- Sơ đồ ```mermaid``` được render thành PNG (docx/hinh/) bằng mermaid-cli + Chrome có sẵn trên máy.
  Sơ đồ không đổi nội dung thì dùng lại ảnh cũ (cache theo hash).
- Chuyển Markdown -> .docx bằng pandoc (gói pypandoc_binary).

Cách dùng (từ thư mục gốc dự án):
    python tools/docs-export/export_docx.py                # xuất từng file docs/*.md
    python tools/docs-export/export_docx.py 01-tong-quan   # chỉ xuất một file
    python tools/docs-export/export_docx.py --merge        # gộp toàn bộ thành một file báo cáo

Cài một lần:
    python -m pip install --user pypandoc_binary
    cd tools/docs-export && set PUPPETEER_SKIP_DOWNLOAD=true && npm install
"""

from __future__ import annotations

import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

import pypandoc

ROOT = Path(__file__).resolve().parents[2]
DOCS_DIR = ROOT / "docs"
OUT_DIR = ROOT / "docx"
IMG_DIR = OUT_DIR / "hinh"
CACHE_FILE = IMG_DIR / ".cache.json"
TOOL_DIR = Path(__file__).resolve().parent
MMDC = TOOL_DIR / "node_modules" / ".bin" / ("mmdc.cmd" if os.name == "nt" else "mmdc")
REFERENCE_DOC = TOOL_DIR / "reference.docx"  # tuỳ chọn: file mẫu định dạng (font, cỡ chữ)

MERGED_NAME = "00-tai-lieu-phan-tich-thiet-ke.docx"
MERGED_TITLE = "Tài liệu phân tích thiết kế — Hệ thống giao bài & chấm bài IELTS trực tuyến"

MERMAID_RE = re.compile(r"```mermaid\s*\n(.*?)```", re.DOTALL)

CHROME_CANDIDATES = [
    os.environ.get("CHROME_PATH", ""),
    os.path.expandvars(r"%LOCALAPPDATA%\Google\Chrome\Application\chrome.exe"),
    r"C:\Program Files\Google\Chrome\Application\chrome.exe",
    r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe",
    shutil.which("google-chrome") or "",
    shutil.which("chromium") or "",
]


def find_browser() -> str:
    for path in CHROME_CANDIDATES:
        if path and Path(path).is_file():
            return path
    sys.exit("Không tìm thấy Chrome/Edge. Đặt biến môi trường CHROME_PATH tới file chrome.exe.")


class MermaidRenderer:
    """Render Mermaid -> PNG, dùng lại ảnh cũ nếu nội dung sơ đồ không đổi."""

    def __init__(self) -> None:
        self.cache: dict[str, str] = json.loads(CACHE_FILE.read_text("utf-8")) if CACHE_FILE.is_file() else {}
        self.cfg_path: Path | None = None
        self.rendered = 0
        self.reused = 0

    def _puppeteer_cfg(self) -> Path:
        if self.cfg_path is None:
            with tempfile.NamedTemporaryFile("w", suffix=".json", delete=False, encoding="utf-8") as cfg:
                json.dump({"executablePath": find_browser(), "args": ["--no-sandbox"]}, cfg)
                self.cfg_path = Path(cfg.name)
        return self.cfg_path

    def render(self, code: str, out_png: Path) -> None:
        digest = hashlib.sha256(code.encode("utf-8")).hexdigest()
        if out_png.is_file() and self.cache.get(out_png.name) == digest:
            self.reused += 1
            return
        with tempfile.NamedTemporaryFile("w", suffix=".mmd", delete=False, encoding="utf-8") as f:
            f.write(code)
            src = f.name
        try:
            subprocess.run(
                [str(MMDC), "-i", src, "-o", str(out_png), "-b", "white", "-s", "2", "-p", str(self._puppeteer_cfg())],
                check=True,
                capture_output=True,
                text=True,
                encoding="utf-8",
                errors="replace",
                shell=os.name == "nt",
            )
        except subprocess.CalledProcessError as e:
            sys.exit(f"Lỗi render Mermaid cho {out_png.name}:\n{e.stderr}")
        finally:
            os.unlink(src)
        self.cache[out_png.name] = digest
        self.rendered += 1

    def close(self) -> None:
        CACHE_FILE.write_text(json.dumps(self.cache, indent=2, ensure_ascii=False), "utf-8")
        if self.cfg_path:
            self.cfg_path.unlink(missing_ok=True)


def prepare(md_path: Path, renderer: MermaidRenderer) -> tuple[str, int]:
    """Đọc file Markdown, thay mỗi khối mermaid bằng ảnh PNG đã render."""
    text = md_path.read_text(encoding="utf-8")
    counter = 0
    prefix = md_path.stem.split("-")[0]

    def replace(match: re.Match[str]) -> str:
        nonlocal counter
        counter += 1
        png = IMG_DIR / f"{md_path.stem}-hinh-{counter}.png"
        renderer.render(match.group(1), png)
        return f"![Hình {prefix}.{counter}]({png.as_posix()})"

    return MERMAID_RE.sub(replace, text), counter


def to_docx(text: str, out: Path, title: str | None = None) -> None:
    args = ["--toc", "--toc-depth=2", f"--resource-path={DOCS_DIR}"]
    if title:
        args += ["--metadata", f"title={title}"]
    if REFERENCE_DOC.is_file():
        args.append(f"--reference-doc={REFERENCE_DOC}")
    pypandoc.convert_text(text, "docx", format="gfm", outputfile=str(out), extra_args=args)


def main() -> None:
    if not MMDC.exists():
        sys.exit("Chưa cài mermaid-cli. Chạy: cd tools/docs-export && npm install")
    OUT_DIR.mkdir(exist_ok=True)
    IMG_DIR.mkdir(exist_ok=True)

    args = sys.argv[1:]
    merge = "--merge" in args
    names = [a for a in args if not a.startswith("--")]
    files = [DOCS_DIR / f"{n.removesuffix('.md')}.md" for n in names] if names else sorted(DOCS_DIR.glob("*.md"))
    missing = [f for f in files if not f.is_file()]
    if missing:
        sys.exit(f"Không tìm thấy: {', '.join(str(m) for m in missing)}")

    renderer = MermaidRenderer()
    try:
        if merge:
            parts = []
            total = 0
            for f in files:
                text, count = prepare(f, renderer)
                parts.append(text)
                total += count
            out = OUT_DIR / MERGED_NAME
            to_docx("\n\n---\n\n".join(parts), out, title=MERGED_TITLE)
            print(f"✔ Gộp {len(files)} tài liệu -> {out.relative_to(ROOT)} ({total} sơ đồ)")
        else:
            for f in files:
                text, count = prepare(f, renderer)
                out = OUT_DIR / f"{f.stem}.docx"
                to_docx(text, out)
                print(f"✔ {f.name} -> {out.relative_to(ROOT)} ({count} sơ đồ)")
        print(f"  Sơ đồ: render mới {renderer.rendered}, dùng lại {renderer.reused}")
    finally:
        renderer.close()


if __name__ == "__main__":
    main()
