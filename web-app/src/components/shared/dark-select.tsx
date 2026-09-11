import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

export interface SelectOption {
  value: string;
  label: string;
}

interface DarkSelectProps {
  value: string;
  onValueChange: (value: string) => void;
  options: SelectOption[];
  placeholder?: string;
  /** form selects sit on var(--background) inputs; toolbar filters on var(--card) cards */
  variant?: "form" | "toolbar";
  width?: string;
}

const triggerBase = "h-8 text-xs border-border focus:ring-accent/40";

const contentStyle = { background: "var(--card)", border: "1px solid var(--border)" };

export function DarkSelect({
  value,
  onValueChange,
  options,
  placeholder,
  variant = "form",
  width,
}: DarkSelectProps) {
  return (
    <Select value={value} onValueChange={onValueChange}>
      <SelectTrigger
        className={`${triggerBase} ${width ?? ""}`}
        style={{
          background: variant === "form" ? "var(--background)" : "var(--card)",
          color: "var(--secondary-foreground)",
        }}
      >
        <SelectValue placeholder={placeholder} />
      </SelectTrigger>
      <SelectContent style={contentStyle}>
        {options.map((option) => (
          <SelectItem
            key={option.value}
            value={option.value}
            className="text-xs"
            style={{ color: "var(--secondary-foreground)" }}
          >
            {option.label}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}
