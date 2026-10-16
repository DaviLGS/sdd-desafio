package reembolso.io;

import static reembolso.io.Json.booleano;
import static reembolso.io.Json.data;
import static reembolso.io.Json.numero;
import static reembolso.io.Json.objeto;
import static reembolso.io.Json.texto;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import reembolso.dominio.Colaborador;
import reembolso.dominio.Despesa;
import reembolso.dominio.Entrada;
import reembolso.dominio.Periodo;

/**
 * Lê e valida o JSON de despesas (spec, seção 4).
 *
 * <p>Os números são lidos como {@link BigDecimal} com os dígitos exatos do arquivo (plano, DT-001).
 * A despesa sai crua: normalização (RN-001) e arredondamento (RN-002) ficam com o motor.
 */
public final class LeitorEntrada {

    public Entrada lerArquivo(Path arquivo) throws EntradaInvalidaException {
        return lerTexto(Json.lerArquivo(arquivo, "de entrada"));
    }

    public Entrada lerTexto(String json) throws EntradaInvalidaException {
        JsonNode raiz = Json.raiz(json, "a entrada");
        JsonNode colaborador = objeto(raiz, "colaborador", "colaborador");
        JsonNode periodo = objeto(raiz, "periodo", "periodo");

        return new Entrada(
                new Colaborador(
                        texto(colaborador, "id", "colaborador.id"),
                        texto(colaborador, "nome", "colaborador.nome"),
                        texto(colaborador, "centro_custo", "colaborador.centro_custo")),
                new Periodo(
                        texto(periodo, "competencia", "periodo.competencia"),
                        data(periodo, "inicio", "periodo.inicio"),
                        data(periodo, "fim", "periodo.fim")),
                despesas(raiz));
    }

    private List<Despesa> despesas(JsonNode raiz) throws EntradaInvalidaException {
        JsonNode lista = raiz.get("despesas");
        if (lista == null || lista.isNull()) {
            throw new EntradaInvalidaException("campo obrigatório ausente: despesas");
        }
        if (!lista.isArray()) {
            throw new EntradaInvalidaException("despesas: deve ser uma lista");
        }

        List<Despesa> despesas = new ArrayList<>();
        for (int i = 0; i < lista.size(); i++) {
            String caminho = "despesas[" + i + "]";
            JsonNode no = Json.comoObjeto(lista.get(i), caminho);
            String categoria = texto(no, "categoria", caminho + ".categoria");
            despesas.add(new Despesa(
                    texto(no, "id", caminho + ".id"),
                    data(no, "data", caminho + ".data"),
                    categoria,
                    categoria,
                    texto(no, "descricao", caminho + ".descricao"),
                    texto(no, "fornecedor", caminho + ".fornecedor"),
                    numero(no, "valor", caminho + ".valor"),
                    booleano(no, "tem_nota_fiscal", caminho + ".tem_nota_fiscal")));
        }
        return despesas;
    }
}
