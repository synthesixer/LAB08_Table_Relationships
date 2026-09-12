# 🛍️ Lab 8: Table Relationships — Product Shop

**วิชา:** CP353002 Principles of Software Design  
**เรื่อง:** ความสัมพันธ์ตาราง 1:1 และ 1:N ด้วย Spring Boot + JPA + PostgreSQL  
**รูปแบบ:** ทำเดี่ยว

---

## 📋 วัตถุประสงค์

1. เข้าใจและอธิบายความแตกต่างของความสัมพันธ์แบบ **One-to-One (1:1)** และ **One-to-Many (1:N)**
2. สร้าง Entity ที่มี `@OneToOne` และ `@OneToMany` / `@ManyToOne` ได้ถูกต้อง
3. เข้าใจว่า **SOLID Principles** เกี่ยวข้องกับการออกแบบ Entity และ Relationship อย่างไร
4. ทำ CRUD ที่ครอบคลุมข้อมูลจากหลายตารางได้
5. ยังคงใช้ Strategy Pattern, Layered Architecture จาก Lab 7

---

## 🧠 ทบทวน SOLID กับการออกแบบตาราง

| Principle | ความหมาย | เกี่ยวกับ Relationship อย่างไร |
|---|---|---|
| **S** — SRP | แต่ละ class/ตารางมีหน้าที่เดียว | แยก `ProductDetail` ออกจาก `Product` — ไม่ยัดทุกอย่างไว้ที่เดียว |
| **O** — OCP | เปิดรับการขยาย ปิดรับการแก้ไข | เพิ่มตาราง `Review` ใหม่ โดยไม่แก้ Entity `Product` |
| **L** — LSP | Subclass ใช้แทน Superclass ได้ | Repository Interface ใช้แทนกันได้ทุก Entity |
| **I** — ISP | Interface ไม่ควรใหญ่เกินไป | แยก `ProductRepository` และ `ReviewRepository` ไม่รวมเป็นตัวเดียว |
| **D** — DIP | ขึ้นกับ Abstraction ไม่ใช่ Implementation | Service ขึ้นกับ `ProductRepository` (interface) ไม่ใช่ class จริง |

---

## 🔗 ความแตกต่าง 1:1 กับ 1:N

### One-to-One (1:1)

```
Product (1) ──────── (1) ProductDetail
```

> Product 1 รายการ มี ProductDetail ได้เพียง 1 รายการ  
> ✅ ใช้เมื่อ: ข้อมูลเสริมที่ไม่ได้ใช้ทุก query — แยกออกเพื่อ **SRP**

### One-to-Many (1:N)

```
Product (1) ──────── (N) Review
```

> Product 1 รายการ มีได้หลาย Review  
> ✅ ใช้เมื่อ: ข้อมูล child ที่เพิ่มได้ไม่จำกัด — ออกแบบให้ **OCP** (เพิ่ม Review โดยไม่แก้ Product)

### กฎ Foreign Key

| ความสัมพันธ์ | FK อยู่ที่ | Annotation |
|---|---|---|
| 1:1 | ฝั่ง Owner (Product มี `detail_id`) | `@OneToOne` + `@JoinColumn` |
| 1:N | ฝั่ง Many (Review มี `product_id`) | `@ManyToOne` + `@JoinColumn` |

---

## 🎯 โจทย์

ต่อยอดจาก Lab 7 เพิ่ม 2 ความสัมพันธ์:

**Part A — 1:1** `Product ↔ ProductDetail`  
ข้อมูลเสริมสินค้า: description, warranty, weight, dimensions

**Part B — 1:N** `Product → Review`  
รีวิวสินค้า: reviewer, rating, comment, reviewDate

---

## 📂 โครงสร้างโปรเจกต์

```
src/main/java/com/example/demo/
├── DemoApplication.java
├── model/
│   ├── Product.java              ← @OneToOne → ProductDetail
│   │                               @OneToMany → List<Review>
│   ├── ProductDetail.java        ← @OneToOne(mappedBy)
│   └── Review.java               ← @ManyToOne → Product (FK: product_id)
├── repository/
│   ├── ProductRepository.java         ← extends JpaRepository (DIP)
│   ├── ProductDetailRepository.java
│   └── ReviewRepository.java
├── strategy/
│   ├── DiscountStrategy.java          (interface — ISP)
│   ├── NoDiscountStrategy.java
│   ├── MemberDiscountStrategy.java    (ลด 10%)
│   ├── SeasonalSaleStrategy.java      (ลด 20%)
│   └── DiscountContext.java
├── service/
│   └── ProductService.java            ← SRP: business logic เท่านั้น
└── controller/
    └── ProductController.java         ← SRP: รับ-ส่ง HTTP เท่านั้น

src/main/resources/
├── application.properties
├── static/css/style.css               ← ✅ มีให้แล้ว
└── templates/products/
    ├── list.html                      ← ✅ มีให้แล้ว
    ├── add.html                       ← ✅ มีให้แล้ว (มีฟอร์ม ProductDetail)
    ├── edit.html                      ← ✅ มีให้แล้ว
    └── delete.html                    ← ✅ มีให้แล้ว
```

---

## 📦 Entity Fields

### Product.java

| Field | Type | Annotation | หมายเหตุ |
|---|---|---|---|
| `id` | Long | `@Id @GeneratedValue` | PK |
| `name` | String | `@Column` | ชื่อสินค้า |
| `category` | String | `@Column` | หมวดหมู่ |
| `brand` | String | `@Column` | ยี่ห้อ |
| `stock` | Integer | `@Column` | จำนวนในคลัง |
| `price` | Double | `@Column` | ราคา |
| `discountType` | String | `@Column` | ประเภทส่วนลด |
| `detail` | ProductDetail | `@OneToOne(cascade=ALL)` + `@JoinColumn` | **1:1** |
| `reviews` | List\<Review\> | `@OneToMany(mappedBy="product")` | **1:N** |

### ProductDetail.java

| Field | Type | หมายเหตุ |
|---|---|---|
| `id` | Long | PK |
| `description` | String | รายละเอียดสินค้า |
| `warranty` | String | รับประกัน เช่น `1 Year` |
| `weight` | Double | น้ำหนัก (kg) |
| `dimensions` | String | ขนาด เช่น `15x7x0.8 cm` |
| `manufacturedCountry` | String | ประเทศที่ผลิต |
| `product` | Product | `@OneToOne(mappedBy="detail")` — inverse |

### Review.java ← ใหม่

| Field | Type | Annotation | หมายเหตุ |
|---|---|---|---|
| `id` | Long | `@Id @GeneratedValue` | PK |
| `reviewer` | String | `@Column` | ชื่อผู้รีวิว |
| `rating` | Integer | `@Column` | คะแนน 1-5 |
| `comment` | String | `@Column` | ความเห็น |
| `reviewDate` | LocalDate | `@Column` | วันที่รีวิว |
| `product` | Product | `@ManyToOne` + `@JoinColumn(name="product_id")` | **FK อยู่ที่นี่** |

---

## 💡 ตัวอย่าง Code Annotation

```java
// Product.java — เจ้าของทั้ง 1:1 และ 1:N
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ... fields อื่นๆ ...

    // ── 1:1 กับ ProductDetail ──
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "detail_id", referencedColumnName = "id")
    private ProductDetail detail;

    // ── 1:N กับ Review ──
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    private List<Review> reviews = new ArrayList<>();
}

// Review.java — ฝั่ง Many เก็บ FK
@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String reviewer;
    private Integer rating;
    private String comment;
    private LocalDate reviewDate;

    // FK อยู่ที่ฝั่ง Many เสมอ
    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;
}
```

---

## 🗄️ Database Setup

### ติดตั้ง PostgreSQL

**Windows:** https://www.postgresql.org/download/windows/ → รัน `.exe` → จำ password ของ `postgres`

**macOS:**
```bash
brew install postgresql@16
brew services start postgresql@16
```

### สร้าง Database

```bash
psql -U postgres
CREATE DATABASE lab8shop;
\q
```

### application.properties

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/lab8shop
spring.datasource.username=postgres
spring.datasource.password=YOUR_PASSWORD_HERE

spring.jpa.show-sql=true
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

> ✅ Spring JPA จะสร้างตาราง `products`, `product_details`, `reviews` พร้อม FK ให้อัตโนมัติ

---

##  วิธีการติดตั้งและรันโปรเจกต์

**สิ่งที่ต้องเตรียมบนเครื่อง**
- Java 21 (หรือเวอร์ชันที่รองรับ Spring Boot 3.x)
- Maven (wrapper `./mvnw` มีให้)
- PostgreSQL 16 (หรือเวอร์ชันที่รองรับ)

### 1. Clone Repository
```bash
git clone https://github.com/yourusername/lab8-673380417-sec1.git
cd lab8-673380417-sec1
```

### 2. ตั้งค่า Database
```sql
CREATE DATABASE lab8shop;
```
แก้ไขไฟล์ `src/main/resources/application.properties` ให้ตรงกับ username/password ของ PostgreSQL

### 3. Build & Run
```bash
./mvnw spring-boot:run   # Windows: mvnw.cmd spring-boot:run
```
แอปจะรันที่ <http://localhost:8080> และ UI ที่ `/products`

### 4. ทดสอบ (optional)
```bash
./mvnw test
```

---



## 🔄 URL Mappings

| Method | URL | หน้าที่ |
|---|---|---|
| GET | `/products` | รายการสินค้า |
| GET | `/products/add` | ฟอร์มเพิ่มสินค้า |
| POST | `/products/save` | บันทึกสินค้า (+ ProductDetail) |
| GET | `/products/edit/{id}` | ฟอร์มแก้ไข |
| POST | `/products/update/{id}` | อัปเดต |
| GET | `/products/delete/{id}` | ยืนยันลบ |
| POST | `/products/delete/{id}` | ลบสินค้า |

---

## 📝 สิ่งที่ต้องส่ง

1. **ลิ้งค์ GitHub Repository** ชื่อ `lab8-{รหัสนักศึกษา}-sec{section}`

2. **PDF รายงาน** แบ่งเป็น 3 ส่วน:

**ส่วนที่ 1: หลักการออกแบบ**
- อธิบายว่าใช้ **SOLID** ส่วนไหนบ้างและเกี่ยวข้องกับ Entity อย่างไร
- อธิบายความแตกต่างของ **1:1 กับ 1:N** — ใช้กรณีไหนควรใช้แบบไหน
- อธิบาย **Strategy Pattern** สำหรับคำนวณส่วนลด
- อธิบาย **Execution Flow** ตั้งแต่ HTTP Request → DB

**ส่วนที่ 2: Code + คำอธิบาย**
- Entity ทั้ง 3 ตัว (`Product`, `ProductDetail`, `Review`) พร้อมอธิบาย annotation
- Service และ Controller พร้อมอธิบาย Constructor Injection

**ส่วนที่ 3: ภาพหน้าจอ**

> 📌 ใส่รหัสนักศึกษาและ Section ในชื่อสินค้า เช่น `iPhone 15 Pro (673380123-4 SEC 1)`

| ภาพที่ต้องถ่าย | รายละเอียด |
|---|---|
| Create | กรอกข้อมูล Product + ProductDetail |
| Read | รายการสินค้าในตาราง |
| Update | หน้าฟอร์มแก้ไข |
| Delete | ยืนยันลบ + หลังลบ |
| Database | pgAdmin แสดง 3 ตาราง (`products`, `product_details`, `reviews`) พร้อม FK |

---

## 📊 เกณฑ์การให้คะแนน

| หัวข้อ | คะแนน |
|---|---|
| SOLID Principles — อธิบายและประยุกต์ใช้ | 20% |
| 1:1 Relationship (Entity + annotation) | 20% |
| 1:N Relationship (Entity + annotation) | 20% |
| Repository, Service, Controller | 15% |
| Strategy Pattern | 10% |
| Database (3 ตาราง + FK ถูกต้อง) | 10% |
| PDF Report | 5% |
| **รวม** | **100%** |