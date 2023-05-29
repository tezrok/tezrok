CREATE TABLE public.author (
  id INT NOT NULL,
  name VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL
);

CREATE TABLE public.book (
  id INT NOT NULL,
  title VARCHAR(255) NOT NULL,
  isbn VARCHAR(255) NOT NULL,
  publicationDate DATE NOT NULL
);
