package com.example.demo;

import com.example.demo.model.Product;
import com.example.demo.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductService productService;

    @Test
    void testRootRedirectsToProducts() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products"));
    }

    @Test
    void testListProducts() throws Exception {
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(view().name("products/list"))
                .andExpect(model().attributeExists("products"));
    }

    @Test
    void testShowAddForm() throws Exception {
        mockMvc.perform(get("/products/add"))
                .andExpect(status().isOk())
                .andExpect(view().name("products/add"))
                .andExpect(model().attributeExists("product"));
    }

    @Test
    void testSaveProduct() throws Exception {
        mockMvc.perform(post("/products/save")
                        .param("name", "Test Item")
                        .param("category", "Test Category")
                        .param("brand", "Test Brand")
                        .param("stock", "5")
                        .param("price", "99.99")
                        .param("discountType", "NONE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products"));
    }

    @Test
    void testEditFormAndUpdate() throws Exception {
        Product product = new Product();
        product.setName("Original");
        product.setPrice(10.0);
        Product saved = productService.saveProduct(product);

        mockMvc.perform(get("/products/edit/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("products/edit"))
                .andExpect(model().attributeExists("product"));

        mockMvc.perform(post("/products/update/" + saved.getId())
                        .param("name", "Updated")
                        .param("price", "20.0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products"));
    }

    @Test
    void testDeleteFormAndAction() throws Exception {
        Product product = new Product();
        product.setName("For Delete");
        product.setPrice(15.0);
        Product saved = productService.saveProduct(product);

        mockMvc.perform(get("/products/delete/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("products/delete"))
                .andExpect(model().attributeExists("product"));

        mockMvc.perform(post("/products/delete/" + saved.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products"));
    }
}
