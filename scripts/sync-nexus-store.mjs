import fs from 'node:fs/promises';

const owner = 'Tresor562';
const token = process.env.NEXUS_REPOS_TOKEN || process.env.GITHUB_TOKEN || '';
const headers = {
  'Accept': 'application/vnd.github+json',
  'X-GitHub-Api-Version': '2022-11-28',
  ...(token ? { Authorization: `Bearer ${token}` } : {})
};

async function gh(url) {
  const res = await fetch(url, { headers });
  if (!res.ok) throw new Error(`${res.status} ${url}`);
  return res.json();
}

async function getRepos() {
  const all = [];
  for (let page = 1; page <= 10; page++) {
    const url = token && process.env.NEXUS_REPOS_TOKEN
      ? `https://api.github.com/user/repos?per_page=100&page=${page}&affiliation=owner&sort=updated`
      : `https://api.github.com/users/${owner}/repos?per_page=100&page=${page}&sort=updated`;
    const batch = await gh(url);
    const owned = batch.filter(r => r.owner?.login?.toLowerCase() === owner.toLowerCase() && !r.archived);
    all.push(...owned);
    if (batch.length < 100) break;
  }
  return all;
}

async function metadata(repo) {
  const url = `https://api.github.com/repos/${owner}/${repo.name}/contents/nexus-store.json`;
  const res = await fetch(url, { headers });
  if (res.status === 404) return null;
  if (!res.ok) throw new Error(`${res.status} ${url}`);
  const data = await res.json();
  if (!data.content) return null;
  const json = JSON.parse(Buffer.from(data.content.replace(/\n/g, ''), 'base64').toString('utf8'));
  if (!json.id || !json.name) throw new Error(`${repo.name}: nexus-store.json doit contenir id et name`);
  return { ...json, repository: repo.html_url, updatedAt: repo.updated_at };
}

const catalogPath = 'nexus-store/catalog.json';
const current = JSON.parse(await fs.readFile(catalogPath, 'utf8'));
const map = new Map(current.map(app => [app.id, app]));
const repos = await getRepos();
let discovered = 0;
for (const repo of repos) {
  try {
    const item = await metadata(repo);
    if (!item) continue;
    map.set(item.id, { ...(map.get(item.id) || {}), ...item });
    discovered++;
  } catch (error) {
    console.warn(`Nexus Store: ${repo.name}: ${error.message}`);
  }
}
const next = [...map.values()].sort((a,b) => (a.name || '').localeCompare(b.name || '', 'fr'));
await fs.writeFile(catalogPath, JSON.stringify(next, null, 2) + '\n');
console.log(`Nexus Store: ${next.length} entrées, ${discovered} dépôt(s) synchronisé(s).`);
