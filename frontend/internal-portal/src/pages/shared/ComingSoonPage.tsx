import { EmptyState } from "@/components/common/EmptyState";

export function ComingSoonPage({
  title,
  description,
  note,
}: {
  title: string;
  description: string;
  note?: string;
}) {
  return (
    <EmptyState
      title={title}
      description={`${description}${note ? ` ${note}` : ""}`}
    />
  );
}
