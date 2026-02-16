package me.choir_backend.service;

import jakarta.annotation.PostConstruct;
import me.choir_backend.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Service
public class MemberKeyGeneratorService {

    @Autowired
    MemberRepository memberRepository;

    private List<String> colors;
    private List<String> artists;
    private final Random random = new Random();

    @PostConstruct
    public void init() throws IOException {
        this.colors = loadLines("data/colors.txt");
        this.artists = loadLines("data/artists.txt");
    }

    public String generateUnique(){
        String memberKey;
        do {
            memberKey = generate();
        } while (memberRepository.findBySecretKey(memberKey).isPresent());

        return memberKey;
    }

    private String generate() {
        String color = colors.get(random.nextInt(colors.size()));
        String artist = artists.get(random.nextInt(artists.size()));
        int number = random.nextInt(90) + 10;
        return String.format("%s-%s-%d", color, artist, number);
    }

    private List<String> loadLines(String path) throws IOException {
        var resource = new ClassPathResource(path);
        try (var is = resource.getInputStream()) {
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            return Arrays.stream(content.split("[,\\n]+"))
                    .map(String::trim)
                    .filter(word -> !word.isEmpty())
                    .map(String::toLowerCase)
                    .toList();
        }
    }
}

