-- Создание таблицы задач
CREATE TABLE IF NOT EXISTS tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    completed BOOLEAN DEFAULT FALSE,
    due_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    priority VARCHAR(10) DEFAULT 'MEDIUM',
    category VARCHAR(100)
);

-- Очистка старых данных
DELETE FROM tasks;

-- Добавление тестовых данных
--INSERT INTO tasks (title, description, due_date, priority, category) VALUES
--('Изучить Java', 'Изучить паттерны проектирования и DAO', DATEADD('DAY', 7, CURRENT_DATE), 'HIGH', 'Обучение'),
--('Сделать покупки', 'Купить продукты на неделю', DATEADD('DAY', 1, CURRENT_DATE), 'MEDIUM', 'Личное'),
--('Подготовить отчет', 'Ежеквартальный отчет по проекту', DATEADD('DAY', 3, CURRENT_DATE), 'HIGH', 'Работа'),
--('Занятие спортом', 'Сходить в тренажерный зал', CURRENT_DATE, 'LOW', 'Здоровье');