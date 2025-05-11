-- Необходимо добавить в решении для работы с JDBC
-- Заготовки миграций - определенный инструмент не используем, просто фиксируем изменения в файле в виде SQL

-- Создание таблицы user_achievements
CREATE TABLE public.user_achievements (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    chat_id BIGINT NOT NULL,
    achievement_name VARCHAR(255) NOT NULL,
    achievement_description TEXT,
    achieved_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE public.user_achievements IS 'Таблица достижений пользователей';
COMMENT ON COLUMN public.user_achievements.id IS 'Id достижения';
COMMENT ON COLUMN public.user_achievements.chat_id IS 'Id чата пользователя';
COMMENT ON COLUMN public.user_achievements.achievement_name IS 'Название достижения';
COMMENT ON COLUMN public.user_achievements.achievement_description IS 'Описание достижения';
COMMENT ON COLUMN public.user_achievements.achieved_at IS 'Дата получения достижения'; 