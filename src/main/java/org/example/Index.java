package org.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

/**
 * Класс представляет из себя заготовку поискового движка в файловой системе. Движок позволяет подсчитывать
 * количество вхождений строки (вводимой пользователем в строке поиска) в содержимое файла и, в зависимости от релевантности,
 * показывать файлы в поисковой выдаче. Похоже, это простая реализация утилиты grep для Linux или findstr для Windows.
 */
public class Index {
    // Поля invertedIndex и pool нужно сделать private, поскольку они представляют собой элементы внутренней реализации.

    /** invertedIndex - базовая структура, хранит в себе поисковую выдачу */
    TreeMap<String, List<Pointer>> invertedIndex;

    /** pool - сервис для создания и управления потоками */
    ExecutorService pool;

    public Index(ExecutorService pool) {
        this.pool = pool;
        // Чтобы при мерже ветки не вылезали конфликты, лучше придерживаться одного стиля написания кода: this.invertedIndex
        invertedIndex = new TreeMap<>();
    }

    /**
     * Функция идет в указанную директорию и собирает список имеющихся в ней файлов.
     * После этого запускает три задачи по поиску. */
    public void indexAllTxtInPath(String pathToDir) throws IOException {
        Path of = Path.of(pathToDir);

        BlockingQueue<Path> files = new ArrayBlockingQueue<>(2);

        try (Stream<Path> stream = Files.list(of)) {
            stream.forEach(files::add);
        }

        // Создаем три задачи. Возможно стоило бы проверить корректность их выполнения.
        // Функция submit возвращает Future<?>, и с помощью него можно сделать проверку.
        pool.submit(new IndexTask(files));
        pool.submit(new IndexTask(files));
        pool.submit(new IndexTask(files));
    }

    /**
     * API движка. Функция возвращает дерево выдачи. */
    public TreeMap<String, List<Pointer>> getInvertedIndex() {
        return invertedIndex;
    }


    /**
     * API движка. Функция позволяет получить дерево выдачи для
     * поискового запроса. */
    public List<Pointer> GetRelevantDocuments(String term) {
        return invertedIndex.get(term);
    }

    /**
     * API движка. Эта функция позволяет получить самый релевантный файл (для поискового запроса),
     * в котором чаще других встречается вводимое пользователем слово. */
    public Optional<Pointer> getMostRelevantDocument(String term) {
        return invertedIndex.get(term).stream().max(Comparator.comparing(o -> o.count));
    }

    /**
     * Вспомогательный класс, который реализует указатель на конкретный файл в файловой
     * системе и хранит количество вхождений в содержимое этого файла строки, вводимой пользователем. */
    static class Pointer {
        // Класс можно сделать private, поскольку он является элементов внутренней реализации
        private Integer count;
        private String filePath;

        public Pointer(Integer count, String filePath) {
            this.count = count;
            this.filePath = filePath;
        }

        @Override
        public String toString() {
            return "{" + "count=" + count + ", filePath='" + filePath + '\'' + '}';
        }
    }

    /**
     * Класс-исполнитель. Позволяет запустить движок в несколько параллельных задач
     * и реализует основной функционал поиска. */
    class IndexTask implements Runnable {
        // Класс можно сделать private, поскольку он является элементов внутренней реализации
        private final BlockingQueue<Path> queue;

        public IndexTask(BlockingQueue<Path> queue) {
            this.queue = queue;
        }


        /**
         * Ключевой функционал. С помощью вспомогательного класса Pointer данная
         * функция считает количество вхождений строки в содержимое файла и сохраняет
         * искомую информацию в базовую структуру invertedIndex. То есть заполняет дерево поисковой выдачи */
        @Override
        public void run() {
            try {
                Path take = queue.take();
                List<String> strings = Files.readAllLines(take);

                strings.stream().flatMap(str -> Stream.of(str.split(" "))).forEach(word -> invertedIndex.compute(word, (k, v) -> {
                    if (v == null) return List.of(new Pointer(1, take.toString()));
                    else {
                        ArrayList<Pointer> pointers = new ArrayList<>();

                        if (v.stream().noneMatch(pointer -> pointer.filePath.equals(take.toString()))) {
                            pointers.add(new Pointer(1, take.toString()));
                        }

                        v.forEach(pointer -> {
                            if (pointer.filePath.equals(take.toString())) {
                                pointer.count = pointer.count + 1;
                            }
                        });

                        pointers.addAll(v);

                        return pointers;
                    }
                }));
            } catch (InterruptedException | IOException e) {
                throw new RuntimeException();
            }
        }
    }
}
