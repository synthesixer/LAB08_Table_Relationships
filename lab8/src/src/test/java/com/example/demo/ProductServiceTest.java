package com.example.demo;

import com.example.demo.model.Product;
import com.example.demo.model.ProductDetail;
import com.example.demo.model.Review;
import com.example.demo.service.ProductService;
import com.example.demo.strategy.DiscountContext;
import com.example.demo.strategy.MemberDiscountStrategy;
import com.example.demo.strategy.NoDiscountStrategy;
import com.example.demo.strategy.SeasonalSaleStrategy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ProductServiceTest {

    @Autowired
    private ProductService productService;

    @Test
    void testStrategyPatternDiscountCalculation() {
        Product pNone = new Product();
        pNone.setPrice(1000.0);
        pNone.setDiscountType("NONE");
        assertEquals(1000.0, pNone.getDiscountedPrice(), 0.001);

        Product pMember = new Product();
        pMember.setPrice(1000.0);
        pMember.setDiscountType("MEMBER");
        assertEquals(900.0, pMember.getDiscountedPrice(), 0.001);

        Product pSeasonal = new Product();
        pSeasonal.setPrice(1000.0);
        pSeasonal.setDiscountType("SEASONAL");
        assertEquals(800.0, pSeasonal.getDiscountedPrice(), 0.001);

        // Directly test Strategy classes with Context
        DiscountContext context = new DiscountContext(new NoDiscountStrategy());
        assertEquals(500.0, context.calculatePrice(500.0), 0.001);

        context.setStrategy(new MemberDiscountStrategy());
        assertEquals(450.0, context.calculatePrice(500.0), 0.001);

        context.setStrategy(new SeasonalSaleStrategy());
        assertEquals(400.0, context.calculatePrice(500.0), 0.001);
    }

    @Test
    void testCreateAndRetrieveProductWithDetailAndReview() {
        Product product = new Product();
        product.setName("iPhone 15 Pro");
        product.setCategory("Smartphone");
        product.setBrand("Apple");
        product.setStock(10);
        product.setPrice(42900.0);
        product.setDiscountType("MEMBER");

        ProductDetail detail = new ProductDetail();
        detail.setDescription("Titanium design with A17 Pro chip");
        detail.setWarranty("1 Year");
        detail.setWeight(0.187);
        detail.setDimensions("14.66 x 7.06 x 0.83 cm");
        detail.setManufacturedCountry("China");
        product.setDetail(detail);

        Review review = new Review();
        review.setReviewer("สมชาย ใจดี");
        review.setRating(5);
        review.setComment("ยอดเยี่ยม ใช้งานดีมาก");
        review.setReviewDate(LocalDate.now());

        List<Review> reviews = new ArrayList<>();
        reviews.add(review);
        product.setReviews(reviews);

        Product saved = productService.saveProduct(product);
        assertNotNull(saved.getId());
        assertNotNull(saved.getDetail().getId());
        assertEquals(1, saved.getReviews().size());
        assertEquals("Apple", saved.getBrand());
        assertEquals(42900.0 * 0.9, saved.getDiscountedPrice(), 0.01);

        Product retrieved = productService.getProductById(saved.getId());
        assertNotNull(retrieved);
        assertEquals("iPhone 15 Pro", retrieved.getName());
        assertEquals("Titanium design with A17 Pro chip", retrieved.getDetail().getDescription());
        assertEquals("สมชาย ใจดี", retrieved.getReviews().get(0).getReviewer());
    }

    @Test
    void testUpdateProduct() {
        Product product = new Product();
        product.setName("Old Name");
        product.setPrice(100.0);
        product.setStock(5);
        product.setDiscountType("NONE");

        Product saved = productService.saveProduct(product);

        Product updateData = new Product();
        updateData.setName("Updated Name");
        updateData.setPrice(120.0);
        updateData.setStock(15);
        updateData.setDiscountType("SEASONAL");

        ProductDetail detail = new ProductDetail();
        detail.setDescription("Updated description");
        updateData.setDetail(detail);

        Product updated = productService.updateProduct(saved.getId(), updateData);
        assertNotNull(updated);
        assertEquals("Updated Name", updated.getName());
        assertEquals(120.0, updated.getPrice());
        assertEquals(120.0 * 0.8, updated.getDiscountedPrice(), 0.01);
        assertEquals("Updated description", updated.getDetail().getDescription());
    }

    @Test
    void testDeleteProduct() {
        Product product = new Product();
        product.setName("To Be Deleted");
        product.setPrice(50.0);
        Product saved = productService.saveProduct(product);
        Long id = saved.getId();

        assertNotNull(productService.getProductById(id));
        productService.deleteProduct(id);
        assertNull(productService.getProductById(id));
    }
}
