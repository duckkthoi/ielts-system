import { useState, type ReactNode } from "react"
import {
  Avatar,
  Button,
  Card,
  Checkbox,
  Collapse,
  Dropdown,
  Input,
  Popover,
  Progress,
  Segmented,
  Select,
  Space,
  Tag,
  Tooltip,
  Typography,
} from "antd"
import type { MenuProps } from "antd"
import {
  Check,
  CheckCircle2,
  ChevronRight,
  Clock3,
  CloudCheck,
  CloudOff,
  LoaderCircle,
  Lock,
  MessageSquareText,
  MoreHorizontal,
  RotateCcw,
  Trash2,
} from "lucide-react"

const { Text, Title, Paragraph } = Typography

export type SubmissionStatus = "NOT_STARTED" | "DRAFT" | "SUBMITTED" | "LATE" | "GRADING" | "RETURNED" | "REVISION_REQUESTED"

const statusMap: Record<SubmissionStatus, {
  label: string
  className: string
}> = {
  NOT_STARTED: { label: "Chưa làm", className: "status-not-started" },
  DRAFT: { label: "Đang làm", className: "status-draft" },
  SUBMITTED: { label: "Đã nộp", className: "status-submitted" },
  LATE: { label: "Nộp muộn", className: "status-late" },
  GRADING: { label: "Đang chấm", className: "status-grading" },
  RETURNED: { label: "Đã trả bài", className: "status-returned" },
  REVISION_REQUESTED: { label: "Cần sửa", className: "status-revision" },
}

export function StatusTag({
  status,
  size = "md",
}: {
  status: SubmissionStatus
  size?: "sm" | "md"
}) {
  const item = statusMap[status]
  return (
    <Tag className={`status-tag ${item.className} status-${size}`}>
      <span className="status-dot" />
      {item.label}
    </Tag>
  )
}

function bandLevel(band: number) {
  if (band <= 4.5) return "weak"
  if (band <= 5.5) return "average"
  if (band <= 6.5) return "fair"
  if (band <= 7.5) return "good"
  return "excellent"
}

export function BandBadge({
  band,
  size = "md",
}: {
  band?: number
  size?: "sm" | "md" | "lg"
}) {
  return (
    <span
      className={`band-badge band-${
        band === undefined ? "empty" : bandLevel(band)
      } band-${size}`}
    >
      {band === undefined ? "—" : band.toFixed(1)}
    </span>
  )
}

export function ScorePicker({
  value,
  compact = false,
  variant = "whole",
  limit,
  limitReason = "Không chia đoạn",
  onChange,
}: {
  value: number
  compact?: boolean
  variant?: "whole" | "half"
  limit?: number
  limitReason?: string
  onChange?: (value: number) => void
}) {
  const options =
    variant === "half"
      ? Array.from({ length: 19 }, (_, index) => index / 2)
      : Array.from({ length: 10 }, (_, index) => index)
  const format = (score: number) =>
    variant === "half" ? score.toFixed(1) : String(score)
  const tooltip = `Tối đa ${limit} vì: ${limitReason}`
  if (compact) {
    return (
      <Select
        aria-label="Chọn điểm"
        className="score-select"
        value={value}
        options={options.map((score) => ({
          label: (
            <span className="score-option-label">
              {limit !== undefined && score > limit ? <Lock /> : null}
              {format(score)}
            </span>
          ),
          value: score,
          disabled: limit !== undefined && score > limit,
        }))}
        onChange={onChange}
      />
    )
  }
  return (
    <Segmented
      aria-label="Chọn điểm"
      block
      className={`score-picker score-picker-${variant} ${
        limit !== undefined ? "score-picker-limited" : ""
      }`}
      value={value}
      options={options.map((score) => ({
        label:
          limit !== undefined && score > limit ? (
            <Tooltip title={tooltip}>
              <span className="score-locked-option">
                <Lock />
                {format(score)}
              </span>
            </Tooltip>
          ) : (
            format(score)
          ),
        value: score,
        disabled: limit !== undefined && score > limit,
      }))}
      onChange={(next) => onChange?.(Number(next))}
    />
  )
}

export function ChecklistItem({
  children,
  kind = "positive",
  checked = false,
  limit,
  disabled = false,
}: {
  children: ReactNode
  kind?: "positive" | "limit"
  checked?: boolean
  limit?: number
  disabled?: boolean
}) {
  return (
    <div
      className={`checklist-item checklist-${kind} ${
        kind === "limit" && checked ? "checklist-limit-active" : ""
      }`}
    >
      <Checkbox defaultChecked={checked} disabled={disabled}>
        {children}
      </Checkbox>
      {kind === "limit" && limit !== undefined ? (
        <Tag className="checklist-limit-tag">Tối đa {limit}</Tag>
      ) : null}
    </div>
  )
}

const descriptorCopy: Record<number, string> = {
  9: "Đáp ứng đầy đủ mọi phần; quan điểm phát triển sâu và thuyết phục.",
  8: "Đáp ứng đầy đủ nhiệm vụ; quan điểm rõ và được phát triển tốt.",
  7: "Trả lời các phần chính; quan điểm rõ; ý được mở rộng nhưng có lúc khái quát quá.",
  6: "Đáp ứng các phần chính; quan điểm phù hợp nhưng phát triển chưa đều.",
  5: "Chỉ đáp ứng một phần nhiệm vụ; ý còn hạn chế hoặc lặp lại.",
  4: "Trả lời tối thiểu; quan điểm chưa rõ và ý chưa được phát triển.",
  3: "Không xử lý đầy đủ nhiệm vụ; ý rất hạn chế hoặc không liên quan.",
  2: "Hầu như không đáp ứng nhiệm vụ; nội dung khó xác định.",
  1: "Chỉ có vài từ rời rạc liên quan đến đề bài.",
  0: "Không làm bài, không dùng tiếng Anh hoặc bài học thuộc hoàn toàn.",
}

export function DescriptorPanel({
  criterion,
  currentScore,
}: {
  criterion: string
  currentScore: number
}) {
  return (
    <Popover
      trigger="click"
      title={`Band descriptors · ${criterion}`}
      content={
        <div className="descriptor-panel">
          {Array.from({ length: 10 }, (_, index) => 9 - index).map((band) => (
            <div
              className={band === currentScore ? "descriptor-active" : ""}
              key={band}
            >
              <Text strong>{band}</Text>
              <Text>{descriptorCopy[band]}</Text>
            </div>
          ))}
          <Button type="link">Bản chính thức (ielts.org)</Button>
        </div>
      }
    >
      <Button
        type="text"
        size="small"
        className="descriptor-trigger"
        aria-label={`Xem band descriptors ${criterion}`}
      >
        ⓘ
      </Button>
    </Popover>
  )
}

const commentBank = [
  ["Quan điểm rõ nhưng cần phát triển ví dụ cụ thể hơn.", 28],
  ["Kiểm tra lại lỗi hoà hợp chủ ngữ – động từ.", 24],
  ["Cần dùng từ nối đa dạng và chính xác hơn.", 19],
  ["Từ vựng phù hợp nhưng còn lặp lại.", 14],
] as const

export function CommentBankPopover({
  onInsert,
}: {
  onInsert?: (comment: string) => void
}) {
  return (
    <Popover
      trigger="click"
      title="Nhận xét mẫu"
      content={
        <div className="comment-bank">
          <Input placeholder="Tìm nhận xét" allowClear />
          {commentBank.map(([comment, used]) => (
            <Button
              type="text"
              block
              key={comment}
              onClick={() => onInsert?.(comment)}
            >
              <span>{comment}</span>
              <Text type="secondary">Đã dùng {used} lần</Text>
            </Button>
          ))}
          <Button type="link">+ Lưu nhận xét này vào ngân hàng</Button>
        </div>
      }
    >
      <Button size="small">Nhận xét mẫu</Button>
    </Popover>
  )
}

export function ErrorCodePicker() {
  const groups = [
    ["LR", ["SP", "WW", "WC", "WF", "COL", "REP"]],
    [
      "GRA",
      ["VT", "SV", "ART", "PREP", "SING/PL", "WO", "P", "FRAG", "RO", "^"],
    ],
    ["CC", ["LINK", "PARA"]],
    ["TA/TR", ["IRR", "DEV"]],
  ] as const
  return (
    <div className="error-codes">
      {groups.map(([group, codes]) => (
        <div key={group}>
          <Text strong>{group}</Text>
          <Space wrap>
            {codes.map((code) => (
              <Button size="small" key={code}>
                {code}
              </Button>
            ))}
          </Space>
        </div>
      ))}
      <div>
        <Text strong>Khác</Text>
        <Space>
          <Button size="small">?</Button>
          <Button size="small" className="good-code">
            GOOD
          </Button>
        </Space>
      </div>
      <Input placeholder="Nhận xét" />
      <Input placeholder="Gợi ý sửa" />
      <div className="suggestion-preview">
        <Text delete>people is</Text>
        <ChevronRight />
        <Text strong>people are</Text>
      </div>
    </div>
  )
}

export function ErrorSummary() {
  return (
    <Card size="small" title="Lỗi đã đánh dấu">
      <Text>GRA – VT ×4, ART ×3 · LR – SP ×2 · CC – LINK ×1</Text>
    </Card>
  )
}

export function CriterionCard({
  completed = false,
  defaultExpanded = true,
}: {
  completed?: boolean
  defaultExpanded?: boolean
}) {
  const [comment, setComment] = useState("")
  return (
    <Card
      className={`criterion-card ${completed ? "criterion-completed" : ""}`}
      size="small"
    >
      <Collapse
        ghost
        defaultActiveKey={defaultExpanded ? ["criterion"] : []}
        items={[
          {
            key: "criterion",
            label: (
              <div className="criterion-heading">
                <div>
                  <Space size={4}>
                    <Text strong>Task Response</Text>
                    <DescriptorPanel
                      criterion="Task Response"
                      currentScore={6}
                    />
                  </Space>
                  <Text type="secondary" className="block-copy">
                    Mức độ trả lời đầy đủ và phát triển quan điểm.
                  </Text>
                </div>
                {completed ? (
                  <CheckCircle2
                    className="success-icon"
                    aria-label="Đã hoàn thành"
                  />
                ) : null}
              </div>
            ),
            children: (
              <Space
                orientation="vertical"
                size="middle"
                className="full-width"
              >
                <Space
                  orientation="vertical"
                  size="small"
                  className="full-width"
                >
                  <ChecklistItem checked>
                    Trả lời đầy đủ tất cả các phần của đề
                  </ChecklistItem>
                  <ChecklistItem checked>
                    Quan điểm rõ ràng và nhất quán
                  </ChecklistItem>
                  <ChecklistItem>
                    Ý chính có giải thích và ví dụ cụ thể
                  </ChecklistItem>
                </Space>
                <ScorePicker value={6} />
                <Input.TextArea
                  rows={2}
                  value={comment}
                  onChange={(event) => setComment(event.target.value)}
                  placeholder="Nhận xét cho tiêu chí này…"
                />
                <CommentBankPopover onInsert={setComment} />
              </Space>
            ),
          },
        ]}
      />
    </Card>
  )
}

export function WordCounter({
  current,
  minimum,
}: {
  current: number
  minimum: number
}) {
  return (
    <Text
      className={current < minimum ? "word-warning" : "word-success"}
      strong
    >
      {current} / {minimum} từ
    </Text>
  )
}

export function AutosaveIndicator({
  state,
}: {
  state: "saved" | "saving" | "offline" | "error"
}) {
  const content = {
    saved: { icon: <CloudCheck />, label: "Đã lưu lúc 14:32" },
    saving: { icon: <LoaderCircle className="spin" />, label: "Đang lưu…" },
    offline: { icon: <CloudOff />, label: "Ngoại tuyến – đã lưu trên máy" },
    error: { icon: <RotateCcw />, label: "Lỗi lưu – thử lại" },
  }[state]
  return (
    <Text className={`autosave autosave-${state}`}>
      {content.icon}
      {content.label}
    </Text>
  )
}

export function CountdownTimer({
  children,
  tone = "neutral",
}: {
  children: ReactNode
  tone?: "neutral" | "warning" | "error"
}) {
  return (
    <Tag className={`countdown countdown-${tone}`}>
      <Clock3 />
      {children}
    </Tag>
  )
}

export function PresenceDot({
  state,
  children,
}: {
  state: "online" | "away" | "offline"
  children?: ReactNode
}) {
  if (!children)
    return <span className={`presence presence-${state}`} aria-label={state} />
  return (
    <span className="presence-wrap">
      {children}
      <span className={`presence presence-${state}`} />
    </span>
  )
}

export function ProgressCell({
  status,
  band,
  pulse = false,
}: {
  status: SubmissionStatus
  band?: number
  pulse?: boolean
}) {
  const item = statusMap[status]
  return (
    <Tooltip title={`Nguyễn Văn An · Task 2 · ${item.label} lúc 23:41 06/11`}>
      <span
        className={`progress-cell ${item.className} ${
          pulse ? "progress-pulse" : ""
        }`}
      >
        {band !== undefined ? (
          band.toFixed(1)
        ) : status === "LATE" ? (
          <Clock3 />
        ) : null}
      </span>
    </Tooltip>
  )
}

export function StatCard({
  label,
  value,
  delta,
}: {
  label: string
  value: string
  delta: string
}) {
  return (
    <Card className="stat-card">
      <Text type="secondary">{label}</Text>
      <Title level={2}>{value}</Title>
      <Text className="positive-copy">{delta}</Text>
    </Card>
  )
}

export function PageHeader({
  title,
  description,
  extra,
}: {
  title: string
  description: string
  extra?: ReactNode
}) {
  return (
    <div className="page-header">
      <div>
        <Text type="secondary">Thư viện giao diện / {title}</Text>
        <Title level={1}>{title}</Title>
        <Paragraph type="secondary">{description}</Paragraph>
      </div>
      {extra}
    </div>
  )
}

export function ChatMessage({
  mine,
  children,
  failed = false,
}: {
  mine?: boolean
  children: ReactNode
  failed?: boolean
}) {
  return (
    <div className={`chat-row ${mine ? "chat-row-mine" : ""}`}>
      <div className={`chat-bubble ${mine ? "chat-mine" : "chat-other"}`}>
        <Text>{children}</Text>
        <Text className="chat-meta">
          {failed
            ? "Không gửi được · Gửi lại"
            : mine
              ? "14:32 · Đã xem"
              : "14:31"}
        </Text>
      </div>
    </div>
  )
}

export function InlineComment() {
  const content = (
    <Space orientation="vertical" className="comment-popover">
      <Text type="secondary">“people is”</Text>
      <Input.TextArea
        defaultValue="Sai hoà hợp chủ ngữ – động từ: people are"
        rows={3}
      />
      <Space className="comment-actions">
        <Button type="text" danger icon={<Trash2 />}>
          Xoá
        </Button>
        <Button type="primary">Lưu</Button>
      </Space>
    </Space>
  )
  return (
    <Popover content={content} title="Nhận xét trong bài" trigger="click">
      <Button type="text" className="inline-comment">
        people is
      </Button>
    </Popover>
  )
}

export const actionMenu: MenuProps["items"] = [
  { key: "edit", label: "Chỉnh sửa" },
  { key: "duplicate", label: "Nhân bản" },
  { type: "divider" },
  { key: "delete", label: "Xoá", danger: true },
]

export function RowActions() {
  return (
    <Dropdown menu={{ items: actionMenu }} trigger={["click"]}>
      <Tooltip title="Thao tác">
        <Button type="text" icon={<MoreHorizontal />} aria-label="Thao tác" />
      </Tooltip>
    </Dropdown>
  )
}

export function CommentMarker() {
  return (
    <Tooltip title="1 nhận xét">
      <Button
        type="text"
        icon={<MessageSquareText />}
        aria-label="Xem nhận xét"
      />
    </Tooltip>
  )
}

export function CompletedMark() {
  return <Check className="success-icon" aria-label="Hoàn thành" />
}
