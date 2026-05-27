import { ReactNode } from "react";
import ReactMarkdown, { Components, defaultUrlTransform } from "react-markdown";

type Alignment = "left" | "center" | "right";

interface FormattedMarkdownProps {
  content: string;
  imageComponent?: Components["img"];
  allowPendingImages?: boolean;
}

interface MarkdownPart {
  content: string;
  alignment?: Alignment;
}

const alignmentClasses: Record<Alignment, string> = {
  left: "text-left",
  center: "text-center",
  right: "text-right",
};

export function FormattedMarkdown({ content, imageComponent, allowPendingImages = false }: FormattedMarkdownProps) {
  const components: Components = {
    a: ({ href, children, node: _node, ...props }) => (
      href === "underline:"
        ? <u className="decoration-2 underline-offset-2">{children}</u>
        : <a href={href} {...props}>{children}</a>
    ),
    ...(imageComponent ? { img: imageComponent } : {}),
  };

  return (
    <>
      {splitAlignedBlocks(content).map((part, index) => {
        const markdown = (
          <ReactMarkdown
            components={components}
            urlTransform={(url) => {
              if (url === "underline:" || (allowPendingImages && url.startsWith("pending-image:"))) {
                return url;
              }
              return defaultUrlTransform(url);
            }}
          >
            {part.content}
          </ReactMarkdown>
        );
        if (!part.alignment) {
          return <MarkdownFragment key={index}>{markdown}</MarkdownFragment>;
        }
        return <div key={index} className={alignmentClasses[part.alignment]}>{markdown}</div>;
      })}
    </>
  );
}

function MarkdownFragment({ children }: { children: ReactNode }) {
  return <>{children}</>;
}

function splitAlignedBlocks(content: string): MarkdownPart[] {
  const parts: MarkdownPart[] = [];
  const block = /^\[align=(left|center|right)\]\n([\s\S]*?)\n\[\/align\](?:\n|$)/gm;
  let previousEnd = 0;
  let match: RegExpExecArray | null;

  while ((match = block.exec(content)) !== null) {
    if (match.index > previousEnd) {
      parts.push({ content: content.slice(previousEnd, match.index) });
    }
    parts.push({ alignment: match[1] as Alignment, content: match[2] });
    previousEnd = block.lastIndex;
  }

  if (previousEnd < content.length || parts.length === 0) {
    parts.push({ content: content.slice(previousEnd) });
  }
  return parts.filter(part => part.content.length > 0);
}
