interface FormErrorProps {
  message: string | null;
}

export function FormError({ message }: FormErrorProps) {
  if (!message) return null;
  return (
    <p
      className="text-sm rounded px-3 py-2"
      style={{
        background: "rgba(241,106,106,0.1)",
        color: "#f16a6a",
        border: "1px solid rgba(241,106,106,0.25)",
      }}
    >
      {message}
    </p>
  );
}
