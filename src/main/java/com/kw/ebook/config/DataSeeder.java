package com.kw.ebook.config;

import com.kw.ebook.entity.vo.Book;
import com.kw.ebook.entity.vo.Member;
import com.kw.ebook.service.BookService;
import com.kw.ebook.service.MemberService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 開發環境種子資料：讓系統一啟動就有可瀏覽／可借閱的資料，便於示範。
 *
 * <p>正規做法應由資料遷移工具（如 Flyway）管理，此處為控制範圍的權宜之計。</p>
 */
@Component
@Profile("dev")
public class DataSeeder implements CommandLineRunner {

    private final BookService bookService;
    private final MemberService memberService;

        public DataSeeder(BookService bookService, MemberService memberService) {
        this.bookService = bookService;
        this.memberService = memberService;
    }

    @Override
    public void run(String... args) {
        bookService.save(new Book(1L, "978-0-13-468599-1",
                "Clean Architecture", "Robert C. Martin",
                "A Craftsman's Guide to Software Structure and Design.", 3, 3));
        bookService.save(new Book(2L, "978-0-596-00712-6",
                "Head First Design Patterns", "Eric Freeman",
                "A brain-friendly guide to design patterns.", 2, 2));
        bookService.save(new Book(3L, "978-0-321-12521-7",
                "Domain-Driven Design", "Eric Evans",
                "On tackling complexity in the heart of software.", 1, 1));
        bookService .save(new Book(4L, "978-0-201-63361-0",
                "Design Patterns", "Gamma, Helm, Johnson, Vlissides",
                "Elements of reusable object-oriented software.", 1, 0)); // 全數借出，示範「無庫存」情境

        memberService.save(new Member(1L, "Alice", Member.DEFAULT_MAX_ACTIVE_LOANS));
        memberService.save(new Member(2L, "Bob", Member.DEFAULT_MAX_ACTIVE_LOANS));
    }
}
