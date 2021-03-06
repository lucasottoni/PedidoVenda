package br.com.pedidovenda.controller;

import java.io.Serializable;
import java.util.List;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.Produces;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotBlank;
import org.primefaces.event.SelectEvent;

import br.com.pedidovenda.model.Cliente;
import br.com.pedidovenda.model.EnderecoEntrega;
import br.com.pedidovenda.model.FormaPagamento;
import br.com.pedidovenda.model.ItemPedido;
import br.com.pedidovenda.model.Pedido;
import br.com.pedidovenda.model.Produto;
import br.com.pedidovenda.model.Usuario;
import br.com.pedidovenda.repository.Clientes;
import br.com.pedidovenda.repository.Produtos;
import br.com.pedidovenda.repository.Usuarios;
import br.com.pedidovenda.service.CadastroPedidoService;
import br.com.pedidovenda.service.NegocioException;
import br.com.pedidovenda.util.jsf.FacesUtil;
import br.com.pedidovenda.validation.PedidoEdicao;
import br.com.pedidovenda.validation.SKU;

@Named
@ViewScoped
public class CadastroPedidoBean implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	@Inject
	private Usuarios usuarios;
	
	@Inject
	private Clientes clientes;
	
	@Inject
	private CadastroPedidoService pedidoService;
	
	@Inject
	private Produtos produtos;
	
	@Inject
	private FacesContext facesContext;
	
	private String sku;
	
	@Produces
	@PedidoEdicao
	private Pedido pedido;
	
	private List<Usuario> vendedores;
	
	private Produto produtoLinhaEditavel;
	
	public CadastroPedidoBean() {
		limpar();
	}
	
	public void inicializar() {
		if (pedido == null) {
			limpar();
		}

		vendedores = usuarios.vendedores();

		pedido.adicionarItemVazio();

		recalcularPedido();
	}
	
	public void clienteSelecionado(SelectEvent event) {
		pedido.setCliente((Cliente) event.getObject());
	}
	
	private void limpar() {
		pedido = new Pedido();
		pedido.setEnderecoEntrega(new EnderecoEntrega());
	}
	
	public void pedidoAlterado(@Observes PedidoAlteradoEvent event) {
		pedido = event.getPedido();
	}
	
	public void salvar() {
		pedido.removerItemVazio();
		
		try {
			pedido = pedidoService.salvar(pedido);
			
			FacesUtil.addInfoMessage("Pedido salvo com sucesso!");
		} catch (NegocioException ne) {
			FacesUtil.addErrorMessage(ne.getMessage());
		} finally {
			pedido.adicionarItemVazio();
		}
	}
	
	public void recalcularPedido() {
		if (pedido != null) {
			pedido.recalcularValorTotal();
		}
	}
	
	public void carregarProdutoPorSku() {
		if (StringUtils.isNotBlank(sku)) {
			produtoLinhaEditavel = produtos.porSku(sku);
			carregarProdutoLinhaEditavel();
		}
	}
	
	public void carregarProdutoLinhaEditavel() {
		ItemPedido item = pedido.getItens().get(0);
		
		if (produtoLinhaEditavel != null) {
			if (existeItemComProduto(produtoLinhaEditavel)) {
				FacesUtil.addErrorMessage("Já existe um item no pedido com o produto informado.");
			} else {
				item.setProduto(produtoLinhaEditavel);
				item.setValorUnitario(produtoLinhaEditavel.getValorUnitario());
				
				pedido.adicionarItemVazio();
				produtoLinhaEditavel = null;
				sku = null;
				
				pedido.recalcularValorTotal();
			}
		}
	}
	
	private boolean existeItemComProduto(Produto produto) {
		boolean existeItem = false;
		
		for (ItemPedido item : getPedido().getItens()) {
			if (produto.equals(item.getProduto())) {
				existeItem = true;
				break;
			}
		}
		
		return existeItem;
	}

	public List<Produto> completarProduto(String nome) {
		return produtos.porNome(nome);
	}
	
	public void atualizarQuantidade(ItemPedido item, int linha) {
		if (item.getQuantidade() < 1) {
			if (linha == 0) {
				item.setQuantidade(1);
			} else {
				getPedido().getItens().remove(linha);
			}
		}
		
		pedido.recalcularValorTotal();
	}
	
	public FormaPagamento[] getFormasPagamento() {
		return FormaPagamento.values();
	}
	
	public List<Cliente> completarCliente(String nome) {
		return clientes.porNome(nome);
	}

	public Pedido getPedido() {
		return pedido;
	}
	public void setPedido(Pedido pedido) {
		this.pedido = pedido;
	}

	public List<Usuario> getVendedores() {
		return vendedores;
	}
	
	public Produto getProdutoLinhaEditavel() {
		return produtoLinhaEditavel;
	}
	public void setProdutoLinhaEditavel(Produto produtoLinhaEditavel) {
		this.produtoLinhaEditavel = produtoLinhaEditavel;
	}

	public boolean isEditando() {
		return getPedido().isExistente();
	}

	@SKU
	public String getSku() {
		return sku;
	}
	public void setSku(String sku) {
		this.sku = sku;
	}
	
	@NotBlank
	public String getNomeCliente() {
		return pedido.getCliente() == null ? null : pedido.getCliente().getNome();
	}
	public void setNomeCliente(String nome) {
	}
	
	/**
	 * Verifica se a fase de renderização é response
	 * @author gilsonsilvati 
	 * -- 01/04/2017 
	 */
	public boolean isPhaseRenderResponse() {
		return facesContext.getCurrentPhaseId().getName().equals("RENDER_RESPONSE");
	}
	
}
