package org.easymapper.sample.mapper;

import org.apache.ibatis.session.RowBounds;
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

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = TestApplication.class)
public class BaseTest {
    @Autowired
    protected ProductMapper productMapper;
    @Autowired
    protected CompanyMapper companyMapper;

    protected RowBounds rowBounds = new RowBounds(0, 100);

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
        companyMapper.findAll(rowBounds).forEach(company -> companyMapper.deleteById(company.getId()));
        productMapper.findAll(rowBounds).forEach(product -> productMapper.deleteById(product.getId()));
    }

    @Test
    public void test() {
    }
}
