insert into channels (name, subscribe_enabled, cancel_enabled, created_at, updated_at)
values ('홈페이지', true, true, now(6), now(6)),
       ('모바일앱', true, true, now(6), now(6)),
       ('네이버', true, false, now(6), now(6)),
       ('SKT', true, false, now(6), now(6)),
       ('콜센터', false, true, now(6), now(6)),
       ('이메일', false, true, now(6), now(6));
