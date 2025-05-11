-- Необходимо добавить в решении для работы с JDBC
-- Заготовки миграций - определенный инструмент не используем, просто фиксируем изменения в файле в виде SQL

-- Создание таблицы user_sessions
CREATE TABLE public.user_sessions (
	id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
	chat_id BIGINT NOT NULL,
	"type" varchar NOT NULL,
	duration int8 NOT NULL,
	start_at timestamp NOT NULL,
	stop_at timestamp,
	completed boolean DEFAULT false
);

COMMENT ON TABLE public.user_sessions IS 'Таблица сессий пользователя - история запуска Pomodoro';

-- Column comments
COMMENT ON COLUMN public.user_sessions.id IS 'Id сессии пользователя';
COMMENT ON COLUMN public.user_sessions.chat_id IS 'Id чата пользователя';
COMMENT ON COLUMN public.user_sessions."type" IS 'Тип сессии';
COMMENT ON COLUMN public.user_sessions.duration IS 'Продолжительность сессии';
COMMENT ON COLUMN public.user_sessions.start_at IS 'Время начала сессии';
COMMENT ON COLUMN public.user_sessions.stop_at IS 'Время окончания сессии';
COMMENT ON COLUMN public.user_sessions.completed IS 'Признак завершения сессии';
