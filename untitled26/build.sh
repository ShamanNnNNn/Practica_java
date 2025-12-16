#!/bin/bash

echo "🧹 Очистка..."
rm -rf target
mkdir -p target/classes

echo "🔧 Компиляция Java файлов..."
javac -cp "lib/sqlite-jdbc-3.44.1.0.jar" \
      -d target/classes \
      src/main/java/org/example/*.java

if [ $? -eq 0 ]; then
    echo "✅ Компиляция успешна!"
    echo "🚀 Запуск программы..."
    java -cp "target/classes:lib/sqlite-jdbc-3.44.1.0.jar" org.example.Main
else
    echo "❌ Ошибка компиляции!"
    exit 1
fi
