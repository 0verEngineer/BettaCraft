package org.overengineer.bettacraft.compat.guideme;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import guideme.Guide;
import guideme.GuideItemSettings;
import guideme.color.ConstantColor;
import guideme.compiler.PageCompiler;
import guideme.compiler.TagCompiler;
import guideme.compiler.tags.FlowTagCompiler;
import guideme.document.flow.LytFlowParent;
import guideme.document.flow.LytFlowSpan;
import guideme.libs.mdast.mdx.model.MdxJsxElementFields;
import guideme.style.TextStyle;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.overengineer.bettacraft.progression.GuideBook;

/**
 * M21.5: the handbook is registered from code (it used to be the data-driven {@code guideme_guides/guide.json}) so it can
 * bring its own tag: {@code <Small>by 0verEngineer</Small>} writes smaller, grey text, e.g. next to the title on the start
 * page. Same id ({@code bettacraft:guide}), folder ({@code guides/bettacraft/guide}) and item settings as before.
 * Client only; callers must check that GuideME is loaded.
 */
public final class GuideMEClientCompat {
    private GuideMEClientCompat() {}

    public static void register() {
        Guide.builder(GuideBook.GUIDE_ID)
                .itemSettings(new GuideItemSettings(Optional.of(Component.translatable("item.bettacraft.guide")),
                        List.of(Component.translatable("item.bettacraft.guide.tooltip").withStyle(ChatFormatting.GRAY)), Optional.empty()))
                .extension(TagCompiler.EXTENSION_POINT, new SmallTag())
                .build();
    }

    /** {@code <Small>text</Small>}: the text at 70 % of the body size (also inside a heading), grey. */
    static final class SmallTag extends FlowTagCompiler {
        static final float SCALE = 0.7f;
        static final int GREY = 0xFF8C8C8C;

        @Override
        public Set<String> getTagNames() {
            return Set.of("Small");
        }

        @Override
        protected void compile(PageCompiler compiler, LytFlowParent parent, MdxJsxElementFields el) {
            var span = new LytFlowSpan();
            span.setStyle(TextStyle.builder().fontScale(SCALE).color(new ConstantColor(GREY)).bold(false).build());
            parent.append(span);
            compiler.compileFlowContext(el.children(), span);
        }
    }
}
