package me.cameronwhyte.pufferfish;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import discord4j.common.JacksonResources;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.RestClient;
import discord4j.rest.service.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class GlobalCommandRegistrar implements ApplicationRunner {

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    private final RestClient client;

    @Value("${bot.staffserver}")
    private long staffServerId;

    public GlobalCommandRegistrar(RestClient client) {
        this.client = client;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        final JacksonResources d4jMapper = JacksonResources.create();
        PathMatchingResourcePatternResolver matcher = new PathMatchingResourcePatternResolver();
        final ApplicationService applicationService = client.getApplicationService();
        final long applicationId = client.getApplicationId().block();
        List<ApplicationCommandRequest> commands = new ArrayList<>();
        List<ApplicationCommandRequest> staffCommands = new ArrayList<>();
        for(Resource resource : matcher.getResources("commands/**/*.json")) {
            byte[] inputStream = resource.getInputStream().readAllBytes();
            String path = String.valueOf(resource);
            if(path.contains("staff")) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode jsonNode = mapper.readValue(inputStream, JsonNode.class);
                jsonNode = ((ObjectNode) jsonNode).put("default_member_permissions", "8");
                inputStream = mapper.writeValueAsBytes(jsonNode);
            }
            ApplicationCommandRequest request = d4jMapper.getObjectMapper().readValue(inputStream, ApplicationCommandRequest.class);
            if(request.defaultMemberPermissions().isPresent()) {
                System.out.printf("Adding staff command %s%n", request.name());
                staffCommands.add(request);
            } else {
                System.out.printf("Adding global command %s%n", request.name());
                commands.add(request);
            }
        }
        applicationService.bulkOverwriteGlobalApplicationCommand(applicationId, commands)
                .doOnNext(ignore -> LOGGER.debug("Successfully registered global commands"))
                .doOnError(error -> LOGGER.error("Unable to register global commands", error))
                .subscribe();
        applicationService.bulkOverwriteGuildApplicationCommand(applicationId, this.staffServerId, staffCommands)
                .doOnNext(ignore -> LOGGER.debug("Successfully registered staff commands"))
                .doOnError(error -> LOGGER.error("Unable to register staff commands", error))
                .subscribe();
        //applicationService.getGlobalApplicationCommands(applicationId)
        //      .flatMap(command -> applicationService.deleteGlobalApplicationCommand(applicationId, command.id().asLong())).next().block();
    }
}
