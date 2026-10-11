package com.unicauca.muisca.infrastructure.mde;

import com.unicauca.muisca.application.port.ArtifactProblem;
import com.unicauca.muisca.domain.model.AtlCompiler;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cabecera de un módulo ATL: compilador ({@code -- @atlcompiler}), nombre del módulo y
 * modelos de entrada/salida ({@code create OUT : Destino from IN : Origen;}).
 */
record AtlHeader(AtlCompiler compiler, String moduleName, String inModelName, String sourceAlias,
        String outModelName, String targetAlias, List<ArtifactProblem> problems) {

    private static final Pattern COMPILER = Pattern.compile("--\\s*@atlcompiler\\s+(\\w+)");
    private static final Pattern COMMENT = Pattern.compile("--[^\\r\\n]*");
    private static final Pattern MODULE = Pattern.compile("\\bmodule\\s+([\\w.]+)\\s*;");
    private static final Pattern CREATE = Pattern.compile("\\bcreate\\s+(.+?)\\s+(from|refining)\\s+(.+?);",
            Pattern.DOTALL);
    private static final Pattern MODEL = Pattern.compile("^\\s*(\\w+)\\s*:\\s*(\\w+)\\s*$");
    private static final Pattern USES = Pattern.compile("\\buses\\s+\\w+\\s*;");

    static AtlHeader parse(String source) {
        List<ArtifactProblem> problems = new ArrayList<>();

        AtlCompiler compiler = AtlCompiler.ATL2006;
        Matcher compilerTag = COMPILER.matcher(source);
        if (compilerTag.find()) {
            String tag = compilerTag.group(1).toLowerCase(Locale.ROOT);
            if (tag.equals("emftvm")) {
                compiler = AtlCompiler.EMFTVM;
            } else if (!tag.startsWith("atl20")) {
                problems.add(ArtifactProblem.warning(lineOf(source, compilerTag.start()),
                        "Compilador '" + tag + "' desconocido; se usará atl2006"));
            }
        }

        String code = COMMENT.matcher(source).replaceAll("");
        String moduleName = null;
        Matcher module = MODULE.matcher(code);
        if (module.find()) {
            moduleName = module.group(1);
        } else {
            problems.add(ArtifactProblem.error(1, "Falta la declaración 'module <Nombre>;' (las librerías ATL no se "
                    + "pueden registrar como transformación)"));
        }
        if (USES.matcher(code).find()) {
            problems.add(ArtifactProblem.error(null, "Los módulos que usan librerías ATL ('uses') no están soportados"));
        }

        String inModel = null, sourceAlias = null, outModel = null, targetAlias = null;
        Matcher create = CREATE.matcher(code);
        if (!create.find()) {
            problems.add(ArtifactProblem.error(null, "Falta la declaración 'create OUT : Destino from IN : Origen;'"));
        } else if (create.group(2).equals("refining")) {
            problems.add(ArtifactProblem.error(null, "Los módulos en modo 'refining' no están soportados"));
        } else {
            String[] outs = create.group(1).split(",");
            String[] ins = create.group(3).split(",");
            Matcher out = MODEL.matcher(outs[0]);
            Matcher in = MODEL.matcher(ins[0]);
            if (outs.length != 1 || ins.length != 1 || !out.matches() || !in.matches()) {
                problems.add(ArtifactProblem.error(null,
                        "Solo se admite un modelo de entrada y uno de salida: 'create OUT : Destino from IN : Origen;'"));
            } else {
                outModel = out.group(1);
                targetAlias = out.group(2);
                inModel = in.group(1);
                sourceAlias = in.group(2);
            }
        }
        return new AtlHeader(compiler, moduleName, inModel, sourceAlias, outModel, targetAlias, problems);
    }

    private static int lineOf(String text, int offset) {
        int line = 1;
        for (int i = 0; i < offset; i++) {
            if (text.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }
}
