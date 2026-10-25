import type { LocalDraft } from '@/lib/utils/offlineDraft';

// ─── Nháp offline trong IndexedDB (docs/06 mục 7, SD-06) ──────────────────────
// Dùng IndexedDB gốc của trình duyệt (không thêm thư viện). Mọi lỗi (chế độ ẩn danh, bị chặn, hết dung lượng)
// đều nuốt: nháp offline chỉ là lớp bảo vệ thêm, không được làm hỏng màn làm bài.

const DB_NAME = 'ielts-writing';
const STORE = 'drafts';

let dbPromise: Promise<IDBDatabase> | null = null;

function openDb(): Promise<IDBDatabase> {
  if (!dbPromise) {
    dbPromise = new Promise((resolve, reject) => {
      const req = indexedDB.open(DB_NAME, 1);
      req.onupgradeneeded = () => req.result.createObjectStore(STORE);
      req.onsuccess = () => resolve(req.result);
      req.onerror = () => reject(req.error);
    });
    dbPromise.catch(() => {
      dbPromise = null;
    });
  }
  return dbPromise;
}

async function run<T>(mode: IDBTransactionMode, action: (store: IDBObjectStore) => IDBRequest<T>): Promise<T | null> {
  try {
    const db = await openDb();
    return await new Promise<T>((resolve, reject) => {
      const req = action(db.transaction(STORE, mode).objectStore(STORE));
      req.onsuccess = () => resolve(req.result);
      req.onerror = () => reject(req.error);
    });
  } catch {
    return null;
  }
}

export const offlineDraftStore = {
  get: async (key: string) => (await run<LocalDraft | undefined>('readonly', (s) => s.get(key))) ?? null,
  put: (key: string, draft: LocalDraft) => run('readwrite', (s) => s.put(draft, key)),
  remove: (key: string) => run('readwrite', (s) => s.delete(key))
};
