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
  /** form selects sit on #0f0f11 inputs; toolbar filters on #17171a cards */
  variant?: "form" | "toolbar";
  width?: string;
}

const triggerBase = "h-8 text-xs border-[#252528] focus:ring-[#e8a020]/40";

const contentStyle = { background: "#17171a", border: "1px solid #252528" };

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
          background: variant === "form" ? "#0f0f11" : "#17171a",
          color: "#a09a92",
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
            style={{ color: "#a09a92" }}
          >
            {option.label}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}
