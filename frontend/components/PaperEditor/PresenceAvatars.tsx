import { Avatar, Tooltip } from 'antd';

import type { PresencePeer } from '@/lib/utils/paperPresence';

// ─── Ai đang mở tài liệu Paper + đang soạn mục nào (màn làm bài + màn giám sát) ─

const initial = (name: string) => name.trim().split(/\s+/).pop()?.charAt(0) ?? '?';

export function PresenceAvatars({
  peers,
  meId,
  sectionTitle
}: {
  peers: PresencePeer[];
  meId?: number;
  sectionTitle: (key: string | null) => string | undefined;
}) {
  if (peers.length === 0) return null;
  return (
    <span className="inline-flex items-center gap-2 text-caption text-neutral-500">
      <Avatar.Group max={{ count: 4 }} size="small">
        {peers.map((p) => {
          const section = sectionTitle(p.section);
          return (
            <Tooltip
              key={p.userId}
              title={`${p.userName}${p.userId === meId ? ' (bạn)' : ''}${section ? ` · đang soạn ${section}` : ''}`}
            >
              <Avatar className="bg-brand-600">{initial(p.userName)}</Avatar>
            </Tooltip>
          );
        })}
      </Avatar.Group>
      {peers.length} đang mở
    </span>
  );
}
