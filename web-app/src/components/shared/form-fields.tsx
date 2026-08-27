import type { ReactNode } from "react";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

const inputClass =
  "border-[#252528] text-[#f0ede8] placeholder:text-[#3a3a3d] focus-visible:ring-[#e8a020]/40 focus-visible:border-[#e8a020]/60";

interface FormFieldProps {
  label: ReactNode;
  children: ReactNode;
}

export function FormField({ label, children }: FormFieldProps) {
  return (
    <div className="space-y-1.5">
      <Label className="text-xs uppercase tracking-wider" style={{ color: "#6b6560" }}>
        {label}
      </Label>
      {children}
    </div>
  );
}

interface TextFieldProps {
  label: ReactNode;
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  type?: string;
  required?: boolean;
  maxLength?: number;
}

export function TextField({
  label,
  value,
  onChange,
  placeholder,
  type = "text",
  required = false,
  maxLength,
}: TextFieldProps) {
  return (
    <FormField label={label}>
      <Input
        type={type}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        required={required}
        maxLength={maxLength}
        className={inputClass}
        style={{ background: "#0f0f11" }}
      />
    </FormField>
  );
}

interface NumberFieldProps {
  label: ReactNode;
  value: number | null;
  onChange: (value: number | null) => void;
  placeholder?: string;
  min?: number;
  required?: boolean;
}

export function NumberField({
  label,
  value,
  onChange,
  placeholder,
  min,
  required = false,
}: NumberFieldProps) {
  return (
    <FormField label={label}>
      <Input
        type="number"
        value={value ?? ""}
        onChange={(e) => onChange(e.target.value === "" ? null : Number(e.target.value))}
        placeholder={placeholder}
        min={min}
        required={required}
        className={inputClass}
        style={{ background: "#0f0f11" }}
      />
    </FormField>
  );
}

interface DateFieldProps {
  label: ReactNode;
  value: string | null;
  onChange: (value: string | null) => void;
}

export function DateField({ label, value, onChange }: DateFieldProps) {
  return (
    <FormField label={label}>
      <Input
        type="date"
        value={value ?? ""}
        onChange={(e) => onChange(e.target.value || null)}
        className={`border-[#252528] text-[#f0ede8] focus-visible:ring-[#e8a020]/40 focus-visible:border-[#e8a020]/60`}
        style={{ background: "#0f0f11", colorScheme: "dark" }}
      />
    </FormField>
  );
}
