import { useState } from "react"
import {
  Alert,
  Avatar,
  Badge,
  Button,
  Card,
  Checkbox,
  Drawer,
  Dropdown,
  Form,
  Input,
  Modal,
  Progress,
  Segmented,
  Space,
  Tabs,
  Tooltip,
  Typography,
} from "antd"
import type { MenuProps } from "antd"
import {
  BarChart3,
  Bell,
  BookOpen,
  Check,
  ChevronDown,
  ClipboardCheck,
  Eye,
  EyeOff,
  FileBarChart,
  FileText,
  GraduationCap,
  KeyRound,
  LayoutDashboard,
  Library,
  LockKeyhole,
  LogOut,
  Menu,
  MessageCircle,
  Monitor,
  Save,
  ShieldCheck,
  Smartphone,
  Sparkles,
  UserCog,
  Users,
  X,
} from "lucide-react"
import { PresenceDot } from "./DesignSystem"

const { Text, Title, Paragraph } = Typography

type ScreenKey = "login" | "password" | "shell" | "notifications"
type Viewport = "desktop" | "mobile"
type LoginState = "default" | "error" | "limited"
type Role = "teacher" | "student" | "admin"

const roleLabels: Record<Role, string> = {
  teacher: "Giáo viên",
  student: "Sinh viên",
  admin: "Quản trị viên",
}

const roleNames: Record<Role, string> = {
  teacher: "ThS. Nguyễn Thị Hạnh",
  student: "Nguyễn Văn An",
  admin: "Quản trị viên",
}

const menuByRole: Record<Role, Array<{
  label: string
  icon: React.ReactNode
  badge?: number
}>> = {
  admin: [
    { label: "Tổng quan", icon: <LayoutDashboard /> },
    { label: "Giáo viên", icon: <GraduationCap /> },
    { label: "Người dùng", icon: <Users /> },
    { label: "Nhật ký hệ thống", icon: <FileText /> },
    { label: "Phiên đăng nhập", icon: <ShieldCheck /> },
  ],
  teacher: [
    { label: "Tổng quan", icon: <LayoutDashboard /> },
    { label: "Lớp học", icon: <Users /> },
    { label: "Bài tập", icon: <BookOpen /> },
    { label: "Chấm bài", icon: <ClipboardCheck />, badge: 12 },
    { label: "Ngân hàng đề", icon: <Library /> },
    { label: "Thống kê", icon: <BarChart3 /> },
    { label: "Trao đổi", icon: <MessageCircle /> },
  ],
  student: [
    { label: "Bài được giao", icon: <BookOpen />, badge: 3 },
    { label: "Kết quả học tập", icon: <FileBarChart /> },
    { label: "Trao đổi", icon: <MessageCircle /> },
  ],
}

const accountItems: MenuProps["items"] = [
  { key: "profile", icon: <UserCog />, label: "Thông tin cá nhân" },
  { key: "password", icon: <KeyRound />, label: "Đổi mật khẩu" },
  { type: "divider" },
  { key: "logout", icon: <LogOut />, label: "Đăng xuất", danger: true },
]

const notificationGroups = [
  {
    group: "Hôm nay",
    items: [
      [
        "Bài tập mới: Tuần 5 – Writing practice",
        "Hạn nộp 23:59 07/11/2026",
        "3 phút trước",
        "assignment",
      ],
      [
        "Cần sửa bài: Tuần 4 – Task 1",
        "Hạn sửa 23:59 10/11. Lời nhắn: Bổ sung đoạn tổng quan.",
        "25 phút trước",
        "revision",
      ],
      [
        "Còn 2 giờ đến hạn nộp",
        "Tuần 5 – Task 2 chưa được nộp",
        "1 giờ trước",
        "deadline",
      ],
    ],
  },
  {
    group: "Hôm qua",
    items: [
      [
        "Đã có kết quả: Tuần 4 – Task 2",
        "Band 6.5",
        "Hôm qua, 16:42",
        "result",
      ],
      [
        "Nguyễn Văn An vừa nộp Task 2",
        "Tuần 5 – Writing practice",
        "Hôm qua, 14:20",
        "submit",
      ],
    ],
  },
  {
    group: "Trước đó",
    items: [
      [
        "Bài tập Tuần 4 đã đóng",
        "32/32 sinh viên đã hoàn thành",
        "04/11/2026",
        "assignment",
      ],
    ],
  },
]

function ScreenToolbar({
  screen,
  setScreen,
  viewport,
  setViewport,
}: {
  screen: ScreenKey
  setScreen: (screen: ScreenKey) => void
  viewport: Viewport
  setViewport: (viewport: Viewport) => void
}) {
  return (
    <Card className="screen-toolbar">
      <div className="screen-toolbar-row">
        <div>
          <Text className="eyebrow">PROMPT 2</Text>
          <Title level={2}>Đăng nhập & khung ứng dụng</Title>
          <Text type="secondary">
            Chọn màn hình và kích thước để kiểm tra các biến thể.
          </Text>
        </div>
        <Segmented
          value={viewport}
          options={[
            { value: "desktop", label: "Desktop", icon: <Monitor /> },
            { value: "mobile", label: "Mobile", icon: <Smartphone /> },
          ]}
          onChange={(value) => setViewport(value as Viewport)}
        />
      </div>
      <Tabs
        activeKey={screen}
        onChange={(key) => setScreen(key as ScreenKey)}
        items={[
          { key: "login", label: "Đăng nhập" },
          { key: "password", label: "Đổi mật khẩu lần đầu" },
          { key: "shell", label: "App shell" },
          { key: "notifications", label: "Thông báo" },
        ]}
      />
    </Card>
  )
}

function LoginScreen({ viewport }: { viewport: Viewport }) {
  const [state, setState] = useState<LoginState>("default")
  return (
    <div className="screen-stage">
      <div className="variant-bar">
        <Text strong>Trạng thái</Text>
        <Segmented
          value={state}
          options={[
            { value: "default", label: "Mặc định" },
            { value: "error", label: "Sai thông tin" },
            { value: "limited", label: "Tạm khoá" },
          ]}
          onChange={(value) => setState(value as LoginState)}
        />
      </div>
      <div className={`device-frame device-${viewport} login-screen`}>
        <div className="login-brand-panel">
          <div className="login-brand-content">
            <span className="login-logo">IW</span>
            <Text className="login-product-name">IELTS Writing</Text>
            <Title level={1}>
              Giao bài và chấm bài IELTS Writing trực tuyến
            </Title>
            <Paragraph>
              Không gian học thuật tập trung cho toàn bộ quá trình luyện viết.
            </Paragraph>
            <Space
              orientation="vertical"
              size="middle"
              className="benefit-list"
            >
              {[
                [<Save />, "Tự lưu bài khi đang viết"],
                [<ClipboardCheck />, "Chấm theo tiêu chí và checklist"],
                [<Bell />, "Thông báo tức thì khi có kết quả"],
              ].map(([icon, label]) => (
                <div className="benefit-item" key={String(label)}>
                  <span className="benefit-icon">{icon}</span>
                  <Text>{label}</Text>
                </div>
              ))}
            </Space>
          </div>
          <Text className="login-copyright">
            © 2026 IELTS Writing · Đại học Việt Nam
          </Text>
        </div>
        <div className="login-form-panel">
          <Card className="login-card">
            <div className="mobile-login-brand">
              <span className="brand-mark">IW</span>
              <Text strong>IELTS Writing</Text>
            </div>
            <Title level={1}>Đăng nhập</Title>
            <Paragraph type="secondary">
              Chào mừng bạn trở lại. Vui lòng nhập thông tin tài khoản.
            </Paragraph>
            {state === "error" ? (
              <Alert
                type="error"
                showIcon
                title="Tên đăng nhập hoặc mật khẩu không đúng"
              />
            ) : null}
            {state === "limited" ? (
              <Alert
                type="warning"
                showIcon
                title="Bạn đã nhập sai quá nhiều lần. Thử lại sau 5 phút."
              />
            ) : null}
            <Form layout="vertical" requiredMark={false}>
              <Form.Item label="Tên đăng nhập / Mã sinh viên">
                <Input
                  size="large"
                  prefix={<UserCog />}
                  placeholder="Nhập tên đăng nhập"
                />
              </Form.Item>
              <Form.Item label="Mật khẩu">
                <Input.Password
                  size="large"
                  prefix={<LockKeyhole />}
                  placeholder="Nhập mật khẩu"
                  iconRender={(visible) => (visible ? <Eye /> : <EyeOff />)}
                />
              </Form.Item>
              <Button
                type="primary"
                size="large"
                block
                disabled={state === "limited"}
              >
                Đăng nhập
              </Button>
            </Form>
            <Text type="secondary" className="login-help">
              Quên mật khẩu? Liên hệ giáo viên hoặc quản trị viên.
            </Text>
          </Card>
        </div>
      </div>
    </div>
  )
}

function FirstPasswordScreen({ viewport }: { viewport: Viewport }) {
  const [password, setPassword] = useState("Writing2026")
  const checks = [
    ["Ít nhất 8 ký tự", password.length >= 8],
    ["Có cả chữ và số", /[A-Za-z]/.test(password) && /\d/.test(password)],
    ["Khác mật khẩu cũ", password !== "12345678"],
  ]
  return (
    <div className="screen-stage">
      <div className={`device-frame device-${viewport} password-screen`}>
        <Card className="password-card">
          <span className="password-icon">
            <KeyRound />
          </span>
          <Title level={1}>Đổi mật khẩu</Title>
          <Paragraph type="secondary">
            Thiết lập mật khẩu riêng để bảo vệ tài khoản của bạn.
          </Paragraph>
          <Alert
            type="info"
            showIcon
            title="Đây là lần đăng nhập đầu tiên, vui lòng đặt mật khẩu mới."
          />
          <Form layout="vertical" requiredMark={false}>
            <Form.Item label="Mật khẩu hiện tại">
              <Input.Password
                size="large"
                prefix={<LockKeyhole />}
                placeholder="Nhập mật khẩu tạm"
              />
            </Form.Item>
            <Form.Item label="Mật khẩu mới">
              <Input.Password
                size="large"
                prefix={<KeyRound />}
                value={password}
                onChange={(event) => setPassword(event.target.value)}
              />
            </Form.Item>
            <div className="password-checks">
              {checks.map(([label, passed]) => (
                <Text
                  key={String(label)}
                  className={passed ? "check-passed" : "check-pending"}
                >
                  <span>{passed ? <Check /> : null}</span>
                  {label}
                </Text>
              ))}
            </div>
            <Form.Item label="Nhập lại mật khẩu mới">
              <Input.Password
                size="large"
                prefix={<KeyRound />}
                defaultValue="Writing2026"
              />
            </Form.Item>
            <Button type="primary" size="large" block icon={<Save />}>
              Lưu mật khẩu
            </Button>
          </Form>
        </Card>
      </div>
    </div>
  )
}

function AppSidebar({
  role,
  collapsed,
  mobile,
}: {
  role: Role
  collapsed: boolean
  mobile?: boolean
}) {
  return (
    <aside
      className={`demo-sidebar ${collapsed ? "demo-sidebar-collapsed" : ""} ${
        mobile ? "demo-sidebar-mobile" : ""
      }`}
    >
      <div className="demo-sidebar-brand">
        <span className="brand-mark">IW</span>
        {!collapsed ? <Text strong>IELTS Writing</Text> : null}
      </div>
      <nav className="demo-menu">
        {menuByRole[role].map((item, index) => {
          const content = (
            <Button
              key={item.label}
              type="text"
              className={`demo-menu-item ${
                index === 0 ? "demo-menu-active" : ""
              }`}
              icon={item.icon}
            >
              {!collapsed ? (
                <>
                  <span>{item.label}</span>
                  {item.badge ? <Badge count={item.badge} /> : null}
                </>
              ) : null}
            </Button>
          )
          return collapsed ? (
            <Tooltip key={item.label} title={item.label} placement="right">
              {content}
            </Tooltip>
          ) : (
            content
          )
        })}
      </nav>
      <div className="sidebar-support">
        <Sparkles />
        {!collapsed ? (
          <>
            <Text strong>Cần trợ giúp?</Text>
            <Text type="secondary">Xem hướng dẫn sử dụng</Text>
          </>
        ) : null}
      </div>
    </aside>
  )
}

function DashboardContent({ role }: { role: Role }) {
  const stats =
    role === "teacher"
      ? [
          ["Bài chờ chấm", "12"],
          ["Bài tập đang mở", "3"],
          ["Sinh viên", "64"],
          ["Band trung bình", "6.1"],
        ]
      : role === "student"
        ? [
            ["Bài cần làm", "2"],
            ["Đã hoàn thành", "12"],
            ["Band gần nhất", "6.5"],
            ["Mục tiêu", "7.0"],
          ]
        : [
            ["Người dùng", "196"],
            ["Giáo viên", "12"],
            ["Lớp học", "18"],
            ["Bài nộp hôm nay", "47"],
          ]
  return (
    <div className="demo-dashboard">
      <div className="dashboard-welcome">
        <div>
          <Text type="secondary">Thứ Hai, 9 tháng 11, 2026</Text>
          <Title level={1}>
            {role === "teacher"
              ? "Chào cô Hạnh, chúc cô một ngày hiệu quả"
              : role === "student"
                ? "Chào An, tiếp tục luyện viết nhé"
                : "Tổng quan hệ thống"}
          </Title>
          <Paragraph type="secondary">
            {role === "teacher"
              ? "Có 12 bài nộp mới đang chờ cô chấm."
              : role === "student"
                ? "Bạn có 2 bài tập cần hoàn thành trong tuần này."
                : "Theo dõi hoạt động và trạng thái vận hành hôm nay."}
          </Paragraph>
        </div>
        <Button type="primary">
          {role === "teacher"
            ? "Tạo bài tập"
            : role === "student"
              ? "Xem bài được giao"
              : "Thêm giáo viên"}
        </Button>
      </div>
      <div className="demo-stat-grid">
        {stats.map(([label, value]) => (
          <Card key={label}>
            <Text type="secondary">{label}</Text>
            <Title level={2}>{value}</Title>
            <Text className="positive-copy">+12% so với tuần trước</Text>
          </Card>
        ))}
      </div>
      <div className="dashboard-panels">
        <Card title={role === "student" ? "Bài tập sắp tới" : "Cần xử lý"}>
          {[
            ["Tuần 5 – Writing practice", 72],
            ["Task 2 – Opinion essay", 48],
            ["Tuần 4 – Line graph", 90],
          ].map(([title, value]) => (
            <div className="dashboard-list-item" key={String(title)}>
              <span className="list-icon">
                <FileText />
              </span>
              <div>
                <Text strong>{title}</Text>
                <Text type="secondary" className="block-copy">
                  IELTS K15 – Lớp A
                </Text>
                <Progress
                  percent={Number(value)}
                  showInfo={false}
                  size="small"
                  strokeColor="var(--brand-600)"
                />
              </div>
            </div>
          ))}
        </Card>
        <Card title="Hoạt động trực tiếp">
          {[
            "Trần Thị Bình vừa nộp Task 1",
            "Nguyễn Văn An đang làm Task 2",
            "Lê Hoàng Cường đã xem nhận xét",
          ].map((item, index) => (
            <div className="activity-item" key={item}>
              <Avatar size="small">{index + 1}</Avatar>
              <div>
                <Text>{item}</Text>
                <Text type="secondary" className="block-copy">
                  {index + 2} phút trước
                </Text>
              </div>
            </div>
          ))}
        </Card>
      </div>
    </div>
  )
}

function ShellScreen({ viewport }: { viewport: Viewport }) {
  const [role, setRole] = useState<Role>("teacher")
  const [collapsed, setCollapsed] = useState(false)
  const [disconnected, setDisconnected] = useState(false)
  const [mobileMenu, setMobileMenu] = useState(false)
  const [kicked, setKicked] = useState(false)
  return (
    <div className="screen-stage">
      <div className="variant-bar shell-variant-bar">
        <Space wrap>
          <Text strong>Vai trò</Text>
          <Segmented
            value={role}
            options={[
              { value: "teacher", label: "Giáo viên" },
              { value: "student", label: "Sinh viên" },
              { value: "admin", label: "Quản trị viên" },
            ]}
            onChange={(value) => setRole(value as Role)}
          />
        </Space>
        <Space wrap>
          <Checkbox
            checked={collapsed}
            disabled={viewport === "mobile"}
            onChange={(event) => setCollapsed(event.target.checked)}
          >
            Thu gọn sidebar
          </Checkbox>
          <Checkbox
            checked={disconnected}
            onChange={(event) => setDisconnected(event.target.checked)}
          >
            Mất kết nối
          </Checkbox>
          <Button danger onClick={() => setKicked(true)}>
            Modal đăng xuất
          </Button>
        </Space>
      </div>
      <div className={`device-frame device-${viewport} shell-frame`}>
        {viewport === "desktop" ? (
          <AppSidebar role={role} collapsed={collapsed} />
        ) : null}
        <div className="demo-app-main">
          <header className="demo-app-header">
            <div className="header-leading">
              {viewport === "mobile" ? (
                <Button
                  type="text"
                  icon={<Menu />}
                  aria-label="Mở menu"
                  onClick={() => setMobileMenu(true)}
                />
              ) : null}
              <div>
                <Text type="secondary">IELTS Writing</Text>
                <Text strong className="block-copy">
                  Tổng quan
                </Text>
              </div>
            </div>
            <Space>
              <div className="connection-status">
                <span
                  className={`connection-dot ${
                    disconnected ? "connection-dot-away" : ""
                  }`}
                />
                <Text>{disconnected ? "Mất kết nối" : "Trực tuyến"}</Text>
              </div>
              <Badge count={3} size="small">
                <Button type="text" icon={<Bell />} aria-label="Thông báo" />
              </Badge>
              <Dropdown menu={{ items: accountItems }} trigger={["click"]}>
                <Button type="text" className="account-button">
                  <PresenceDot state={disconnected ? "away" : "online"}>
                    <Avatar>{roleNames[role].charAt(0)}</Avatar>
                  </PresenceDot>
                  <span className="account-copy">
                    <Text strong>{roleNames[role]}</Text>
                    <Text type="secondary">{roleLabels[role]}</Text>
                  </span>
                  <ChevronDown />
                </Button>
              </Dropdown>
            </Space>
          </header>
          {disconnected ? (
            <Alert
              banner
              type="warning"
              showIcon
              title="Mất kết nối… đang thử lại (lần 3)"
            />
          ) : null}
          <DashboardContent role={role} />
        </div>
      </div>
      <Drawer
        rootClassName="mobile-nav-drawer"
        placement="left"
        size={280}
        open={mobileMenu}
        onClose={() => setMobileMenu(false)}
        closeIcon={<X />}
      >
        <AppSidebar role={role} collapsed={false} mobile />
      </Drawer>
      <Modal
        title="Tài khoản vừa đăng nhập ở nơi khác"
        open={kicked}
        closable={false}
        footer={
          <Button type="primary" block onClick={() => setKicked(false)}>
            Đăng nhập lại
          </Button>
        }
      >
        <div className="kicked-modal-content">
          <span className="kicked-icon">
            <Monitor />
          </span>
          <Paragraph>Bạn đã bị đăng xuất khỏi thiết bị này.</Paragraph>
          <Text type="secondary">
            Nếu đây không phải là bạn, hãy đổi mật khẩu sau khi đăng nhập.
          </Text>
        </div>
      </Modal>
    </div>
  )
}

function NotificationItem({
  item,
  unread,
}: {
  item: string[]
  unread: boolean
}) {
  const [title, body, time, type] = item
  return (
    <div
      className={`notification-page-item ${
        unread ? "notification-page-unread" : ""
      }`}
    >
      <span className={`notification-type-icon notification-${type}`}>
        {type === "result" ? <ClipboardCheck /> : <Bell />}
      </span>
      <div className="notification-page-copy">
        <Text strong>{title}</Text>
        <Text type="secondary">{body}</Text>
      </div>
      <div className="notification-page-meta">
        {unread ? <span className="unread-dot" /> : null}
        <Text type="secondary">{time}</Text>
      </div>
    </div>
  )
}

function NotificationsScreen({ viewport }: { viewport: Viewport }) {
  const [toast, setToast] = useState(true)
  const all = (
    <div>
      {notificationGroups.map((group, groupIndex) => (
        <div className="notification-group" key={group.group}>
          <Text className="notification-group-title">{group.group}</Text>
          <Card>
            {group.items.map((item, index) => (
              <NotificationItem
                item={item}
                unread={groupIndex === 0 && index < 3}
                key={item[0]}
              />
            ))}
          </Card>
        </div>
      ))}
    </div>
  )
  return (
    <div className="screen-stage">
      <div className={`device-frame device-${viewport} notification-screen`}>
        <header className="notification-page-header">
          <div>
            <Text type="secondary">Trang chủ / Thông báo</Text>
            <Title level={1}>Thông báo</Title>
            <Paragraph type="secondary">
              Theo dõi cập nhật mới nhất về bài tập và kết quả.
            </Paragraph>
          </div>
          <Button type="link">Đánh dấu đã đọc tất cả</Button>
        </header>
        <Tabs
          defaultActiveKey="all"
          items={[
            { key: "all", label: "Tất cả", children: all },
            {
              key: "unread",
              label: "Chưa đọc (3)",
              children: (
                <Card>
                  {notificationGroups[0].items.map((item) => (
                    <NotificationItem item={item} unread key={item[0]} />
                  ))}
                </Card>
              ),
            },
          ]}
        />
        {toast ? (
          <Card className="realtime-toast">
            <span className="toast-icon">
              <Bell />
            </span>
            <div>
              <Text strong>Bài tập mới</Text>
              <Text type="secondary" className="block-copy">
                Tuần 5 – Writing practice
              </Text>
            </div>
            <Button
              type="text"
              icon={<X />}
              aria-label="Đóng"
              onClick={() => setToast(false)}
            />
          </Card>
        ) : null}
      </div>
    </div>
  )
}

export function Prompt2Screens({ hidden = false }: { hidden?: boolean }) {
  const [screen, setScreen] = useState<ScreenKey>("login")
  const [viewport, setViewport] = useState<Viewport>("desktop")
  return (
    <main className={`prompt2-main ${hidden ? "is-hidden" : ""}`}>
      <ScreenToolbar
        screen={screen}
        setScreen={setScreen}
        viewport={viewport}
        setViewport={setViewport}
      />
      {screen === "login" ? <LoginScreen viewport={viewport} /> : null}
      {screen === "password" ? (
        <FirstPasswordScreen viewport={viewport} />
      ) : null}
      {screen === "shell" ? <ShellScreen viewport={viewport} /> : null}
      {screen === "notifications" ? (
        <NotificationsScreen viewport={viewport} />
      ) : null}
    </main>
  )
}
