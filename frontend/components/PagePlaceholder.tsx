// Khung tạm cho các trang chưa có thiết kế. Thay bằng giao diện thật khi có bản Figma.

export function PagePlaceholder({ title, description }: { title: string; description?: string }) {
  return (
    <div className="p-6">
      <h1 className="text-xl font-semibold">{title}</h1>
      {description && <p className="mt-1 text-sm opacity-70">{description}</p>}
    </div>
  );
}
