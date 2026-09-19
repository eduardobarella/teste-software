import { test, expect } from '@playwright/test';

const casos = [
  { senha: 'Abcde123', confirmacao: 'Abcde123', aceito: true, mensagem: 'Senha cadastrada', classe: 'senha válida no limite mínimo de tamanho (8)' },
  { senha: 'Abcdefghij1234567890', confirmacao: 'Abcdefghij1234567890', aceito: true, mensagem: 'Senha cadastrada', classe: 'senha válida no limite máximo de tamanho (20)' },
  { senha: 'Abc12345', confirmacao: 'Abc12345', aceito: true, mensagem: 'Senha cadastrada', classe: 'senha válida de tamanho intermediário' },
  { senha: 'Abcde12', confirmacao: 'Abcde12', aceito: false, mensagem: 'Senha fora do padrão', classe: 'tamanho abaixo do mínimo (7)' },
  { senha: 'Abcdefghij12345678901', confirmacao: 'Abcdefghij12345678901', aceito: false, mensagem: 'Senha fora do padrão', classe: 'tamanho acima do máximo (21)' },
  { senha: 'abcdefg1', confirmacao: 'abcdefg1', aceito: false, mensagem: 'Senha fora do padrão', classe: 'sem letra maiúscula' },
  { senha: 'ABCDEFG1', confirmacao: 'ABCDEFG1', aceito: false, mensagem: 'Senha fora do padrão', classe: 'sem letra minúscula' },
  { senha: 'Abcdefgh', confirmacao: 'Abcdefgh', aceito: false, mensagem: 'Senha fora do padrão', classe: 'sem dígito numérico' },
  { senha: 'Abcde 12', confirmacao: 'Abcde 12', aceito: false, mensagem: 'Senha fora do padrão', classe: 'contém espaço' },
  { senha: 'Abcde123', confirmacao: 'Abcde124', aceito: false, mensagem: 'As senhas não coincidem', classe: 'formato válido, mas confirmação divergente' },
];

for (const caso of casos) {
  test(`senha "${caso.senha}" — ${caso.classe}`, async ({ page }) => {
    await page.goto('/senha');
    await page.getByLabel('Nova senha').fill(caso.senha);
    await page.getByLabel('Confirmar senha').fill(caso.confirmacao);
    await page.getByRole('button', { name: 'Cadastrar senha' }).click();

    const resultado = page.locator('#resultado');
    await expect(resultado).toBeVisible();
    await expect(resultado).toHaveText(caso.mensagem);
    await expect(resultado).toHaveAttribute('role', caso.aceito ? 'status' : 'alert');
  });
}
