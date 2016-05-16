package org.easymapper.sample.mapper;

import org.apache.ibatis.session.RowBounds;
import org.easymapper.mapper.Criteria;
import org.easymapper.sample.TestApplication;
import org.easymapper.sample.domain.Company;
import org.easymapper.sample.domain.Product;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = TestApplication.class)
public class BaseTest {
    @Autowired
    protected CompanyMapper companyMapper;
    @Autowired
    protected ProductMapper productMapper;

    public Company getCompany() {
        Company company = new Company();
        company.setName(UUID.randomUUID().toString());
        return company;
    }

    public Product getProduct(Long companyId) {
        Product product = new Product();
        product.setCompanyId(companyId);
        product.setName(UUID.randomUUID().toString());

        return product;
    }

    @Before
    public void setup() {
    }

    @After
    public void teardown() {
    }

    @Test
    public void crudTest() {
        Company company = getCompany();
        assertThat(companyMapper.insert(company)).isEqualTo(1);
        Product product1 = getProduct(company.getId());
        Product product2 = getProduct(company.getId());
        assertThat(productMapper.insert(product1)).isEqualTo(1);
        assertThat(productMapper.insert(product2)).isEqualTo(1);
        Product product = new Product();
        product.setCompanyId(company.getId());
        product = productMapper.selectOne(product);
        assertThat(product.getCompanyId()).isEqualTo(company.getId());
        product = new Product();
        product.setCompanyId(company.getId());
        assertThat(productMapper.selectAll(product)).hasSize(2);
        assertThat(productMapper.count(product)).isEqualTo(2);
        assertThat(productMapper.select(product, new RowBounds(0, 1))).hasSize(1);
        assertThat(productMapper.select(product, new RowBounds(0, 2))).hasSize(2);
        assertThat(productMapper.select(product, new RowBounds(1, 2))).hasSize(1);
        Criteria criteria = new Criteria.Builder(Product.class)
                .equalTo("companyId", company.getId())
                .equalTo("id", product1.getId())
                .orderBy("id", Criteria.Direction.ASC)
                .or()
                .equalTo("name", product.getName())
                .build();
        assertThat(productMapper.selectAllByCriteria(criteria)).hasSize(1);
        assertThat(productMapper.selectByCriteria(criteria, new RowBounds(0, 1))).hasSize(1);
        assertThat(productMapper.selectByCriteria(criteria, new RowBounds(0, 0))).hasSize(0);
        product1.setName("new name");
        assertThat(productMapper.updateByPrimaryKey(product1)).isEqualTo(1);
        assertThat(product1.getName()).isEqualTo("new name");
        product = new Product();
        product.setName("new name");
        Product condition = new Product();
        condition.setCompanyId(company.getId());
        assertThat(productMapper.update(product, condition)).isEqualTo(2);
        assertThat(productMapper.updateByCriteria(product, criteria)).isEqualTo(1);
        assertThat(companyMapper.delete(new Company())).isEqualTo(1);
        assertThat(productMapper.deleteByCriteria(criteria)).isEqualTo(1);
        assertThat(productMapper.delete(new Product())).isEqualTo(1);
    }
}
