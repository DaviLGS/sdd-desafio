package reembolso.io;

/** A entrada não respeita o formato da seção 4 da spec e não pode ser processada (plano, DT-005). */
public class EntradaInvalidaException extends Exception {

    public EntradaInvalidaException(String mensagem) {
        super(mensagem);
    }

    public EntradaInvalidaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
