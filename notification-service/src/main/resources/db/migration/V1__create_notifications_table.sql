CREATE TABLE notifications (
   event_id UUID PRIMARY KEY,
   correlation_id UUID NOT NULL,
   core_item_id UUID NOT NULL,
   owner_user_id UUID NOT NULL,
   payload TEXT,
   created_at TIMESTAMPTZ NOT NULL
);