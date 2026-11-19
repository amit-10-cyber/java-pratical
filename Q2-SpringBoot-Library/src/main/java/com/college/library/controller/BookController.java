package com.college.library.controller;

import com.college.library.model.Book;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/books")
public class BookController {

    // In-memory collection: Map gives O(1) lookup by id, LinkedHashMap keeps insertion order
    private final Map<Integer, Book> books = new LinkedHashMap<>();

    public BookController() {
        books.put(1, new Book(1, "Core Java", "Cay Horstmann", "Programming", 5));
        books.put(2, new Book(2, "Effective Java", "Joshua Bloch", "Programming", 3));
    }

    // b) Add a new book
    @PostMapping
    public ResponseEntity<?> addBook(@RequestBody Book book) {
        if (book.getId() == null) {
            return ResponseEntity.badRequest().body("Book id is required");
        }
        if (books.containsKey(book.getId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Book with id " + book.getId() + " already exists");
        }
        books.put(book.getId(), book);
        return ResponseEntity.status(HttpStatus.CREATED).body(book);
    }

    // c) Get all books
    @GetMapping
    public List<Book> getAllBooks() {
        return new ArrayList<>(books.values());
    }

    // c) Get book by id
    @GetMapping("/{id}")
    public ResponseEntity<?> getBookById(@PathVariable Integer id) {
        Book book = books.get(id);
        if (book == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Book with id " + id + " not found");
        }
        return ResponseEntity.ok(book);
    }

    // d) Update book details
    @PutMapping("/{id}")
    public ResponseEntity<?> updateBook(@PathVariable Integer id, @RequestBody Book updated) {
        Book existing = books.get(id);
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Book with id " + id + " not found");
        }
        existing.setTitle(updated.getTitle());
        existing.setAuthor(updated.getAuthor());
        existing.setCategory(updated.getCategory());
        existing.setAvailableCopies(updated.getAvailableCopies());
        return ResponseEntity.ok(existing);
    }

    // e) Delete book by id
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteBook(@PathVariable Integer id) {
        if (books.remove(id) == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Book with id " + id + " not found");
        }
        return ResponseEntity.ok("Book with id " + id + " deleted successfully");
    }
}
