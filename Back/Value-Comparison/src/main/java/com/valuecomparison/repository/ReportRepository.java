package com.valuecomparison.repository;

import com.valuecomparison.model.Report;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportRepository extends MongoRepository<Report, String>{

}
