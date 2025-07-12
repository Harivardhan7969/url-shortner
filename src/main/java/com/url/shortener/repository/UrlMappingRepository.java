package com.url.shortener.repository;

import com.url.shortener.models.UrlMapping;
import com.url.shortener.models.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UrlMappingRepository extends MongoRepository<UrlMapping, String> {

    UrlMapping findByShortUrl(String shortUrl);

    List<UrlMapping> findByUser(User user);

    Optional<UrlMapping> findByShortUrlAndUser(String shortUrl, User user);

    boolean existsByShortUrl(String newShortUrl);
}
