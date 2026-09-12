# รายงาน Lab 8 – ความสัมพันธ์ตาราง (Table Relationships)

**รายบุคคล**
- **ชื่อ**: นาย พุฒิเมธ ชมศรีสวัสดิ์
- **รหัสนักศึกษา**: 673380417‑1
- **สาขา**: Computer Science
- **ภาคการศึกษา**: ปริญญาตรี ปีที่ 3
- **วิชา**: Principles of Software Design (มหาวิทยาลัยขอนแก่น)

---

## ส่วนที่ 1: หลักการออกแบบ

### 1. SOLID Principles ที่ใช้ในโครงการ
| Principle | คำอธิบาย | การนำไปใช้ใน Entity / Layer | ตัวอย่างในโค้ด |
|-----------|-----------|----------------------------|----------------|
| **S – Single Responsibility Principle (SRP)** | ทุกคลาส/ตารางควรมีหน้าที่เดียว | แยก `ProductDetail` ออกจาก `Product` เพื่อเก็บข้อมูลเสริมที่ไม่จำเป็นต้องเรียกทุกครั้ง; `Review` เป็นตารางแยกที่เก็บรีวิวของสินค้า | `@OneToOne` ระหว่าง `Product` ↔ `ProductDetail` (owner) <br/> `@OneToMany` ระหว่าง `Product` ↔ `Review` |
| **O – Open/Closed Principle (OCP)** | สามารถขยายได้โดยไม่ต้องแก้ไขโค้ดเดิม | สามารถเพิ่มตาราง `Review` ใหม่โดยไม่ต้องแก้ไข `Product` เนื่องจาก `Product` มีเพียงการอ้างอิง `List<Review>` | การเพิ่มคลาส `Review` และ Repository ใหม่ |
| **L – Liskov Substitution Principle (LSP)** | Subclass สามารถแทนที่ Superclass ได้ | Repository ทั้งหมดสืบทอดจาก `JpaRepository` ซึ่งเป็น abstraction ที่สอดคล้องกับ LSP |
| **I – Interface Segregation Principle (ISP)** | Interface ควรมีขนาดเล็กและเจาะจง | แยก `ProductRepository`, `ProductDetailRepository`, `ReviewRepository` เป็น Interface แยกกัน ไม่รวมไว้ในไฟล์เดียว |
| **D – Dependency Inversion Principle (DIP)** | ชั้นบนควรพึ่งพา abstraction ไม่ใช่ implementation | Service ชั้น `ProductService` พึ่งพา `ProductRepository` (interface) ผ่าน Constructor Injection |

### 2. ความแตกต่างของความสัมพันธ์ 1:1 กับ 1:N
- **One‑to‑One (1:1)**
  - **โครงสร้าง**: `Product (owner) ──► ProductDetail` (FK อยู่ที่ `Product.detail_id`)
  - **เมื่อใช้**: ข้อมูลเสริมที่ต้องการเก็บแยกออกเพื่อไม่ให้ `Product` มีคอลัมน์มากเกินไป → รองรับ SRP และทำให้ query ที่ไม่ต้องการรายละเอียดเสริมเร็วขึ้น
  - **ข้อดี**: ตาราง `Product` ยังคงเรียบง่าย, สามารถจัดการ Cascade ได้ง่าย (CascadeType.ALL)
- **One‑to‑Many (1:N)**
  - **โครงสร้าง**: `Product (parent) ──► Review (child)` (FK อยู่ที่ `Review.product_id`)
  - **เมื่อใช้**: มีข้อมูลหลายแถวที่เกี่ยวข้องกับหนึ่งแถวหลัก เช่น รีวิวหลายรายการของสินค้าเดียว
  - **ข้อดี**: สามารถเพิ่ม/ลบรีวิวได้อิสระโดยไม่กระทบโครงสร้าง `Product` → สอดคล้องกับ OCP

### 3. Strategy Pattern สำหรับคำนวณส่วนลด
```java
public interface DiscountStrategy {
    double calculateDiscountedPrice(double price);
}

public class NoDiscountStrategy implements DiscountStrategy {
    public double calculateDiscountedPrice(double price) { return price; }
}

public class MemberDiscountStrategy implements DiscountStrategy {
    public double calculateDiscountedPrice(double price) { return price * 0.90; } // ลด 10%
}

public class SeasonalSaleStrategy implements DiscountStrategy {
    public double calculateDiscountedPrice(double price) { return price * 0.80; } // ลด 20%
}

public class DiscountContext {
    private DiscountStrategy strategy;
    public DiscountContext(DiscountStrategy strategy) { this.strategy = strategy; }
    public double calculate(double price) { return strategy.calculateDiscountedPrice(price); }
}
```
- **เหตุผลเลือกใช้**: ส่วนลดอาจเปลี่ยนแปลงบ่อย (โปรโมชั่น, สมาชิกระดับต่าง ๆ) → ต้องการให้ส่วนคำนวณส่วนลดเปิดรับการขยาย (OCP) โดยไม่ต้องแก้ไขโค้ดอื่น ๆ
- **การใช้งาน**: ใน `ProductService` จะสร้าง `DiscountContext` ด้วย Strategy ที่เลือกตาม `discountType` ของสินค้า

### 4. Execution Flow (HTTP Request → Database)
```
[Client] --HTTP GET/POST--> [Spring MVC DispatcherServlet]
    └─> [ProductController] (รับ Request, แปลงเป็น DTO)
        └─> [ProductService] (business logic)
            ├─> เลือก DiscountStrategy → DiscountContext → คำนวนราคาส่วนลด
            └─> Repository Layer (ProductRepository, ReviewRepository, …)
                └─> JPA/Hibernate สร้าง/อ่าน/อัพเดต Entity
                    └─> SQL → PostgreSQL (ตาราง products, product_details, reviews)
        ←─ Response (Model + View) → Thymeleaf Template (list/add/edit) → HTML
```
- **DispatcherServlet** ทำหน้าที่เป็น Front‑Controller
- **Controller** มีหน้าที่รับ/ส่ง HTTP เท่านั้น (SRP)
- **Service** ทำ Business Logic เช่น การคำนวนส่วนลด, การจัดการสัมพันธ์ (SRP)
- **Repository** เป็น abstraction ของการเข้าถึง DB (DIP)
- **Hibernate** แปล Entity ↔ SQL

---

## ส่วนที่ 2: Code + คำอธิบาย

### 1. Entity – Product.java
```java
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String category;
    @Column(nullable = false)
    private String brand;
    @Column(nullable = false)
    private Integer stock;
    @Column(nullable = false)
    private Double price;
    @Column(name = "discount_type")
    private String discountType;

    // ── 1:1 กับ ProductDetail ──
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "detail_id", referencedColumnName = "id")
    private ProductDetail detail;

    // ── 1:N กับ Review ──
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Review> reviews = new ArrayList<>();

    // getters & setters (omitted for brevity)
}
```
**คำอธิบาย Annotation**
- `@Entity` & `@Table` ทำให้คลาสเป็น JPA Entity และกำหนดชื่อตาราง
- `@Id` + `@GeneratedValue` กำหนดคอลัมน์ PK ที่ auto‑increment
- `@Column` ระบุคอลัมน์และคุณสมบัติเช่น `nullable`
- `@OneToOne` + `@JoinColumn` สร้างความสัมพันธ์ 1:1, ฝั่ง Owner คือ `Product` มี FK `detail_id` ชี้ไปยัง PK ของ `ProductDetail`
- `@OneToMany` กับ `mappedBy = "product"` แสดงว่า `Review` เป็นฝั่ง Many ที่เก็บ FK `product_id`

### 2. Entity – ProductDetail.java
```java
@Entity
@Table(name = "product_details")
public class ProductDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String description;
    private String warranty;
    private Double weight;
    private String dimensions;
    private String manufacturedCountry;

    // inverse side of 1:1 relationship
    @OneToOne(mappedBy = "detail", cascade = CascadeType.ALL)
    private Product product;

    // getters & setters
}
```
**คำอธิบาย**
- เป็น Entity ที่เก็บข้อมูลเสริมของสินค้า
- `@OneToOne(mappedBy = "detail")` เป็น inverse side (ไม่มี FK ของตัวเอง)

### 3. Entity – Review.java
```java
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

    // FK อยู่ที่ฝั่ง Many
    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    // getters & setters
}
```
**คำอธิบาย**
- `@ManyToOne` ทำให้หลายรีวิวสามารถอ้างอิงสินค้าเดียวได้
- `@JoinColumn(name = "product_id")` สร้างคอลัมน์ FK ในตาราง `reviews`

### 4. Service – ProductService.java
```java
@Service
@RequiredArgsConstructor // Lombok ทำ Constructor Injection ให้เอง
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductDetailRepository productDetailRepository;
    private final ReviewRepository reviewRepository;

    // ตัวอย่างการสร้างสินค้า พร้อม DiscountStrategy
    public Product createProduct(ProductDto dto) {
        Product product = new Product();
        // set basic fields ...
        product.setName(dto.getName());
        // ... other fields

        // ตั้ง DiscountStrategy ตาม discountType ของสินค้า
        DiscountStrategy strategy = switch (dto.getDiscountType()) {
            case "MEMBER" -> new MemberDiscountStrategy();
            case "SEASONAL" -> new SeasonalSaleStrategy();
            default -> new NoDiscountStrategy();
        };
        double finalPrice = new DiscountContext(strategy).calculate(dto.getPrice());
        product.setPrice(finalPrice);

        // ตั้ง ProductDetail (cascade)
        ProductDetail detail = new ProductDetail();
        detail.setDescription(dto.getDetailDescription());
        // ... set other detail fields
        product.setDetail(detail);

        return productRepository.save(product); // cascade จะบันทึก ProductDetail ด้วย
    }

    // CRUD อื่น ๆ (omitted)
}
```
**คำอธิบาย Constructor Injection**
- ใช้ `@RequiredArgsConstructor` (จาก Lombok) หรือเขียน Constructor เองเพื่อรับ Repository ที่เป็น Interface (abstraction) → ปฏิบัติตาม DIP
- ทำให้ Service ไม่พึ่งพา implementation ของ Repository โดยตรง

### 5. Controller – ProductController.java
```java
@Controller
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public String listProducts(Model model) {
        model.addAttribute("products", productService.findAll());
        return "products/list";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("productDto", new ProductDto());
        return "products/add";
    }

    @PostMapping("/add")
    public String addProduct(@ModelAttribute ProductDto productDto) {
        productService.createProduct(productDto);
        return "redirect:/products";
    }

    // edit / delete (omitted)
}
```
**คำอธิบาย**
- `@Controller` ทำหน้าที่รับ HTTP request และส่ง Model ไปยัง Thymeleaf view
- ใช้ Constructor Injection (`private final ProductService productService;`) เพื่อให้ Controller พึ่งพา Service ผ่าน abstraction เท่านั้น (SRP, DIP)
- ทุกเมธอดทำหน้าที่เดียว: เตรียมข้อมูลเพื่อแสดงผลหรือเรียก Service ทำงาน

---

## ส่วนที่ 3: สรุปและอ้างอิง
- โครงสร้างแบบ Layered (Controller → Service → Repository → Entity) ทำให้ระบบแยกความรับผิดชอบชัดเจนและง่ายต่อการทดสอบ (unit test)
- การใช้ SOLID ร่วมกับ JPA ทำให้ Entity มีความยืดหยุ่นต่อการเปลี่ยนแปลง schema ในอนาคต
- Strategy Pattern ช่วยให้ระบบส่วนลดสามารถเพิ่มโปรโมชั่นใหม่ได้โดยไม่ต้องแก้โค้ด Service หรือ Controller
- Execution Flow ที่อธิบายไว้ช่วยให้เข้าใจขั้นตอนจาก UI → DB และเป็นแนวทางการดีบักเมื่อเกิดปัญหา

**อ้างอิงโค้ด** (ใช้จากโครงการปัจจุบัน)
- `src/main/java/com/example/demo/model/*.java`
- `src/main/java/com/example/demo/service/ProductService.java`
- `src/main/java/com/example/demo/controller/ProductController.java`
- `src/main/java/com/example/demo/repository/*.java`

---

