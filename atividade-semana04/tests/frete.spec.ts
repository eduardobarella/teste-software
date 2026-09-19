import { test, expect } from '@playwright/test';

const casos = [
  { cep: '80000000', valor: '150,00', aceito: true, mensagem: 'Frete: R$ 15,00', classe: 'CEP iniciado por 8, valor abaixo de R$ 200' },
  { cep: '04000000', valor: '150,00', aceito: true, mensagem: 'Frete: R$ 25,00', classe: 'CEP não iniciado por 8, valor abaixo de R$ 200' },
  { cep: '04000000', valor: '200,00', aceito: true, mensagem: 'Frete grátis', classe: 'valor no limite mínimo do frete grátis (R$ 200,00)' },
  { cep: '04000000', valor: '199,99', aceito: true, mensagem: 'Frete: R$ 25,00', classe: 'valor logo abaixo do limite de frete grátis (R$ 199,99)' },
  { cep: '8000000', valor: '150,00', aceito: false, mensagem: 'Dados inválidos', classe: 'CEP com 7 dígitos (abaixo do limite)' },
  { cep: '800000000', valor: '150,00', aceito: false, mensagem: 'Dados inválidos', classe: 'CEP com 9 dígitos (acima do limite)' },
  { cep: '0400000A', valor: '150,00', aceito: false, mensagem: 'Dados inválidos', classe: 'CEP com caractere não numérico' },
  { cep: '04000000', valor: '0', aceito: false, mensagem: 'Dados inválidos', classe: 'valor do pedido não positivo' },
  { cep: '04000000', valor: 'abc', aceito: false, mensagem: 'Dados inválidos', classe: 'valor do pedido em formato inválido' },
  { cep: '', valor: '', aceito: false, mensagem: 'Dados inválidos', classe: 'campos vazios' },
];

for (const caso of casos) {
  test(`frete — CEP "${caso.cep || '(vazio)'}" / valor "${caso.valor || '(vazio)'}" — ${caso.classe}`, async ({ page }) => {
    await page.goto('/frete');
    await page.getByLabel('CEP').fill(caso.cep);
    await page.getByLabel('Valor do pedido').fill(caso.valor);
    await page.getByRole('button', { name: 'Calcular frete' }).click();

    const resultado = page.locator('#resultado');
    await expect(resultado).toBeVisible();
    await expect(resultado).toHaveText(caso.mensagem);
    await expect(resultado).toHaveAttribute('role', caso.aceito ? 'status' : 'alert');
  });
}
