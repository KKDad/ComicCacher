export default function AuthLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="min-h-dvh flex items-center justify-center bg-canvas p-4">
      <div className="w-full max-w-[420px]">
        {children}
      </div>
    </div>
  );
}
