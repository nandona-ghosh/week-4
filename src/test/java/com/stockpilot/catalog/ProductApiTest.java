package com.stockpilot.catalog;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
@SpringBootTest @AutoConfigureMockMvc
class ProductApiTest {
    @Autowired MockMvc mvc;
    @Autowired ProductRepository repository;
    private static final String BASE="/api/v1/products";
    private static final String VALID="""
        {"sku":"PEN-01","name":"Blue pen","unit":"piece","reorderPoint":5}
        """;
    @BeforeEach void clean() { repository.deleteAll(); }
    private Product product(String sku) { return repository.saveAndFlush(new Product(new ProductRequest(sku,"Blue pen","piece",5))); }
    @Test void createReturnsCreatedProductAndLocation() throws Exception {
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(VALID))
          .andExpect(status().isCreated()).andExpect(header().string("Location",org.hamcrest.Matchers.matchesPattern(".*/api/v1/products/\\d+")))
          .andExpect(jsonPath("$.id").isNumber()).andExpect(jsonPath("$.sku").value("PEN-01"));
    }
    @Test void listReturnsProducts() throws Exception {
        product("A"); product("B");
        mvc.perform(get(BASE)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
    }
    @Test void getReturnsProductOrNotFound() throws Exception {
        Product p=product("A");
        mvc.perform(get(BASE+"/{id}",p.getId())).andExpect(status().isOk()).andExpect(jsonPath("$.sku").value("A"));
        mvc.perform(get(BASE+"/999999")).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
    @Test void updateReplacesFieldsAndMissingProductIsNotFound() throws Exception {
        Product p=product("A");
        mvc.perform(put(BASE+"/{id}",p.getId()).contentType(MediaType.APPLICATION_JSON).content(VALID))
          .andExpect(status().isOk()).andExpect(jsonPath("$.sku").value("PEN-01")).andExpect(jsonPath("$.reorderPoint").value(5));
        mvc.perform(put(BASE+"/999999").contentType(MediaType.APPLICATION_JSON).content(VALID)).andExpect(status().isNotFound());
    }
    @Test void deleteRemovesProductAndMissingProductIsNotFound() throws Exception {
        Product p=product("A");
        mvc.perform(delete(BASE+"/{id}",p.getId())).andExpect(status().isNoContent());
        mvc.perform(get(BASE+"/{id}",p.getId())).andExpect(status().isNotFound());
        mvc.perform(delete(BASE+"/{id}",p.getId())).andExpect(status().isNotFound());
    }
    @Test void validationAndMalformedJsonReturnBadRequest() throws Exception {
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(VALID.replace("PEN-01","   ")))
          .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{" )).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_JSON"));
    }
    @Test void duplicateSkuIsRejectedOnCreateAndUpdate() throws Exception {
        product("PEN-01"); Product other=product("OTHER");
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(VALID)).andExpect(status().isConflict());
        mvc.perform(put(BASE+"/{id}",other.getId()).contentType(MediaType.APPLICATION_JSON).content(VALID)).andExpect(status().isConflict());
    }
}
