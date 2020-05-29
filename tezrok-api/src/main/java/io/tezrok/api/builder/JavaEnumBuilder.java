package io.tezrok.api.builder;

import io.tezrok.api.ExecuteContext;
import io.tezrok.api.builder.type.EnumNodeType;
import io.tezrok.api.builder.type.Type;
import io.tezrok.api.model.node.EnumItemNode;
import io.tezrok.api.model.node.EnumNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.velocity.VelocityContext;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Ruslan Absalyamov
 * @since 1.0
 */
public abstract class JavaEnumBuilder extends JavaClassBuilder {
    private final EnumNode enumNode;

    protected JavaEnumBuilder(Type type, ExecuteContext context) {
        super(type, JMod.PUBLIC, context);
        enumNode = ((EnumNodeType) type).getEnumNode();
    }

    @Override
    protected void onBuild(VelocityContext context) {
        super.onBuild(context);

        context.put("enums", getEnums());
    }

    public List<EnumValue> getEnums() {
        List<EnumValue> list = enumNode.items().stream()
                .map(EnumValue::new)
                .collect(Collectors.toList());

        if (!list.isEmpty()) {
            list.get(list.size() - 1).setLast(true);
        }

        return list;
    }

    public static class EnumValue {
        private final EnumItemNode value;
        private boolean last;

        public EnumValue(EnumItemNode value) {
            this.value = Validate.notNull(value, "value");
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();

            if (StringUtils.isNotBlank(value.getDescription())) {
                sb.append("\t/**\n" +
                        "\t * " + value.getDescription() + "\n" +
                        "\t */\n");
            }

            sb.append("\t");
            sb.append(value.getName());

            if (!last) {
                sb.append(',');
            }

            return sb.toString();
        }

        public void setLast(boolean last) {
            this.last = last;
        }
    }
}
