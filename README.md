<<<<<<< HEAD
# NEXA-ACADEMIA

A Java-based University Management System with a console interface and MySQL database integration.

## Features

- Admin, Professor and Student roles
- User/account management
- Course management and professor assignment
- Student course enrollment
- Grades and grade distribution
- Attendance management
- Timetable management
- Notifications and messaging
- Data export functionality
- Academic chatbot / information retrieval
- Custom data-structure implementations:
  - Dynamic Array (`MyArrayList`)
  - Singly Linked List (`MyLinkedList`)
  - Queue (`MyQueue`)
  - Stack (`MyStack`)
- SHA-256 password hashing

## Technologies

- Java
- MySQL
- JDBC
- SQL
- Object-oriented programming
- Data structures

## Project Structure

```text
NEXA-ACADEMIA/
├── src/
│   └── UniversityManagementSystem.java
├── README.md
└── .gitignore
```

## Requirements

- JDK 8+ (a recent JDK is recommended)
- MySQL Server
- MySQL Connector/J JDBC driver

## Database

The application connects to a MySQL database named `university`.

Before running the application, make sure MySQL is running and the database exists:

```sql
CREATE DATABASE university;
```

The application creates its required tables when it starts.

## Configuration

The source currently contains local database configuration for:

```text
jdbc:mysql://localhost:3306/university
user: root
```

For a public repository, use environment variables or another local configuration approach rather than committing a real database password.

## Running

Compile the Java source with the MySQL Connector/J JAR on the classpath, then run:

```bash
javac -cp "path/to/mysql-connector-j.jar" -d out src/UniversityManagementSystem.java
java -cp "out:path/to/mysql-connector-j.jar" UniversityManagementSystem
```

=======
# Nexa-Academia
