package org.easymapper.sample.mapper;

import org.easymapper.sample.domain.Company;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CompanyMapperTest extends BaseTest {
    @Test
    public void crudTest() {
        companyMapper.create(getCompany());
        companyMapper.create(getCompany());
        List<Company> companies = companyMapper.findAll(rowBounds);
        assertThat(companies).hasSize(2);
        assertThat(companyMapper.countAll()).isEqualTo(2);
        Company company = companies.get(0);
        company.setName("new name");
        companyMapper.update(company);
        company = companyMapper.findById(company.getId());
        assertThat(company.getName()).isEqualTo("new name");
        assertThat(companyMapper.findByIds(null)).isEmpty();
        List<Long> ids = new ArrayList<>();
        assertThat(companyMapper.findByIds(ids)).isEmpty();
        ids.add(companies.get(0).getId());
        ids.add(companies.get(1).getId());
        assertThat(companyMapper.findByIds(ids)).hasSize(2);
    }
}
