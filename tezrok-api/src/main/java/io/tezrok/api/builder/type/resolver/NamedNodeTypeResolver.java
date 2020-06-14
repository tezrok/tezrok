package io.tezrok.api.builder.type.resolver;

import io.tezrok.api.builder.type.NamedNodeType;
import io.tezrok.api.builder.type.Type;
import io.tezrok.api.model.node.EntityNode;
import io.tezrok.api.model.node.ModuleNode;
import io.tezrok.api.model.node.ProjectNode;
import org.apache.commons.lang3.Validate;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

public class NamedNodeTypeResolver extends BaseTypeResolver {
    private final ModuleNode module;
    private final ProjectNode project;

    public NamedNodeTypeResolver(ModuleNode module,
                                 ProjectNode project) {
        this(module, project, null);
    }

    public NamedNodeTypeResolver(ModuleNode module,
                                 ProjectNode project,
                                 TypeResolver nextResolver) {
        super(nextResolver);
        this.module = module;
        this.project = project;
    }

    @Override
    public Optional<Type> tryResolveByName(String typeName) {
        Validate.notBlank(typeName, "typeName");
        List<ModuleNode> modules = Arrays.asList(module);

        modules.addAll(project.modules()
                .stream()
                .filter(p -> module != p)
                .collect(toList()));

        for (ModuleNode module : modules) {
            Optional<EntityNode> entity = module.entities().stream()
                    .filter(p -> typeName.equals(p.getName()))
                    .findFirst();

            if (entity.isPresent()) {
                return Optional.of(new NamedNodeType(entity.get(), module.getPackagePath()));
            }
        }

        return super.tryResolveByName(typeName);
    }
}
