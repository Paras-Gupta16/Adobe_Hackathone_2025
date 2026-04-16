# Adobe Hackathon 2025 – PDF Intelligence Suite

Welcome to the **Adobe Hackathon 2025 Repository**! This project is a powerful **PDF Intelligence Suite** consisting of two major modules:

* 🔹 **Adobe_1A – PDF Outline Extractor**
* 🔹 **Adobe_1B – Persona & Relevance Extractor**

Together, these modules transform static PDF documents into **structured, meaningful, and actionable data**.

---



# 🧠 Project Overview

Modern documents (reports, research papers, manuals) contain **hidden structure and context**. This project extracts:

* 📑 Document outlines (Table of Contents)
* 🧑 Persona & intent from content
* 📊 Section-wise relevance scoring

✨ Result: Convert PDFs into **machine-readable intelligence**.

---

# 🏗️ System Architecture

```
                ┌──────────────────────────┐
                │      Input PDFs          │
                │ (Reports / Manuals etc.) │
                └──────────┬───────────────┘
                           │
           ┌───────────────▼───────────────┐
           │        Adobe_1A Module        │
           │  PDF Outline Extractor        │
           │  - Extract bookmarks          │
           │  - Build hierarchy            │
           └───────────────┬───────────────┘
                           │
                           ▼
           ┌───────────────────────────────┐
           │ Structured Outline (JSON)     │
           └───────────────┬───────────────┘
                           │
           ┌───────────────▼───────────────┐
           │        Adobe_1B Module        │
           │ Persona & Section Analyzer    │
           │  - Persona inference          │
           │  - Keyword extraction         │
           │  - Section ranking            │
           └───────────────┬───────────────┘
                           │
                           ▼
           ┌───────────────────────────────┐
           │  Final Structured Output      │
           │ (Ranked Sections + Metadata)  │
           └───────────────────────────────┘
```

---

# 🔹 Module 1: Adobe_1A – PDF Outline Extractor

## 🎯 Purpose

Extracts **document structure (bookmarks/TOC)** programmatically.

## ⚙️ Key Features

* 📌 Extract hierarchical outline from PDFs
* ⚡ Fast and automated batch processing
* 📦 Export structured data (JSON)
* 🔍 Useful for indexing, navigation, and analytics

## 🧰 Tech Stack

* Apache PDFBox (2.0.29)
* Jackson Databind (2.17.1)
* SLF4J Simple (2.0.13)

## 📂 Workflow

1. Place PDFs in `input/`
2. Run application
3. Get structured outline in `output/`

---

# 🔹 Module 2: Adobe_1B – Persona Extractor

## 🎯 Purpose

Transforms documents into **context-aware insights** by understanding:

* Who is the document for?
* What is the goal?

## ⚙️ Key Features

* 🧑 Persona inference from document intro
* 🎯 Job-to-be-done extraction
* 🧠 Heuristic heading detection (font-based)
* 🔑 Keyword-based relevance scoring
* 📊 Ranked sections output

## 🧰 Tech Stack

* Apache PDFBox
* Jackson Databind
* SLF4J

## 📂 Workflow

1. Read first PDF → infer persona
2. Extract headings (H1/H2/H3)
3. Compute keyword relevance
4. Generate ranked JSON output

---

# 📁 Project Structure

```
Adobe_Hackathone_2025/
│
├── Adobe_1A/
├── Adobe_1B/
│
├── input/                # Input PDFs
├── output/               # Generated results
│
├── src/
│   └── main/java/org/example/
│
├── Dockerfile
├── pom.xml
└── README.md
```

---

# ⚙️ Getting Started

## ✅ Prerequisites

* Java JDK 8+
* Maven 3+
* Docker (optional)

---

## 🔨 Build Project

```bash
git clone https://github.com/Paras-Gupta16/Adobe_Hackathone_2025.git
cd Adobe_1A
mvn clean install
```

---

## ▶️ Run Application

```bash
java -jar target/Adobe_1A-1.0-SNAPSHOT-shaded.jar
```

Repeat similarly for `Adobe_1B`.

---

# 🐳 Docker Usage

## Build Image

```bash
docker build -t adobe-extractor .
```

## Run Container

```bash
docker run -v "$(pwd)/input:/app/input" \
           -v "$(pwd)/output:/app/output" \
           adobe-extractor
```

---

# 💡 Use Cases

* 📚 Smart document navigation systems
* 🤖 AI-powered document summarization
* 🗂️ Knowledge base creation
* 🔍 Research paper analysis
* 📊 Enterprise document indexing

---

# 🚀 Future Enhancements

* 🔗 Integration with search engines (Elasticsearch)
* 🧠 NLP-based semantic ranking
* 🌐 Web dashboard for visualization
* ⚡ Real-time document processing

---

# 👨‍💻 Author

**Paras Gupta**

---

# ⭐ Support

If you like this project:

* ⭐ Star the repository
* 🍴 Fork it
* 🤝 Contribute

---

# 🏁 Conclusion

This project bridges the gap between **static PDFs and intelligent systems**, enabling:

✔ Structured extraction
✔ Context understanding
✔ Intelligent ranking

✨ Turning documents into **data-driven insights**.
