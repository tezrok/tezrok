CREATE TABLE public.author (
  id SERIAL PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL
);

CREATE TABLE public.book (
  id SERIAL PRIMARY KEY,
  title VARCHAR(255) NOT NULL,
  isbn VARCHAR(255) NOT NULL,
  publication_date DATE NOT NULL,
  books_author_id INT
);

-- foreign keys
ALTER TABLE public.book ADD CONSTRAINT fk_book_books_author_id FOREIGN KEY(books_author_id) REFERENCES public.author(id);
