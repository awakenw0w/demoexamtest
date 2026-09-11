const $ = (selector, root = document) => root.querySelector(selector);
const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];

const ui = {
  left: $('#leftNumber'),
  right: $('#rightNumber'),
  output: $('#mathOutput'),
  input: $('#personName'),
  roster: $('#roster'),
  add: $('#insertPerson'),
  scan: $('#scanPerson'),
  undo: $('#rollback'),
  dialog: $('#symbolDialog'),
  clean: $('#removeGarbage'),
  notice: $('#notice')
};

const appState = {
  people: ['Алексей', 'Андрей', 'Варвара'],
  active: null,
  snapshots: []
};

function notify(text) {
  ui.notice.textContent = text;
  ui.notice.hidden = false;

  clearTimeout(notify.timer);
  notify.timer = setTimeout(() => {
    ui.notice.hidden = true;
  }, 1700);
}

function readPair() {
  const first = ui.left.value;
  const second = ui.right.value;

  if (first === '' || second === '') {
    notify('Нужно заполнить оба аргумента');
    return;
  }

  return [Number(first), Number(second)];
}

function runCalculation(mode) {
  const pair = readPair();
  if (!pair) return;

  const [a, b] = pair;
  const operations = {
    sum: { sign: '+', value: a + b },
    difference: { sign: '−', value: a - b }
  };

  const result = operations[mode];
  ui.output.textContent = `${a} ${result.sign} ${b} = ${result.value}`;
}

$$('[data-calc]').forEach(button => {
  button.onclick = () => runCalculation(button.dataset.calc);
});

function rememberRoster() {
  appState.snapshots.push(appState.people.slice());
  if (appState.snapshots.length > 15) appState.snapshots.shift();
}

function formatName(source) {
  const compact = source.split(/\s+/).join('');
  if (!compact) return '';

  const lowered = compact.toLocaleLowerCase('ru-RU');
  return lowered[0].toLocaleUpperCase('ru-RU') + lowered.slice(1);
}

function containsGarbage(source) {
  return !/^[a-zA-Zа-яА-ЯёЁ\s-]+$/.test(source);
}

function stripGarbage(source) {
  return source.replace(/[^a-zA-Zа-яА-ЯёЁ\s-]/g, '');
}

function isAlreadyStored(name) {
  const target = name.toLocaleLowerCase('ru-RU');
  return appState.people.some(item => item.toLocaleLowerCase('ru-RU') === target);
}

function drawRoster() {
  const fragment = document.createDocumentFragment();

  appState.people.forEach((person, index) => {
    const row = document.createElement('div');
    row.className = 'roster-item';
    row.dataset.index = index;

    if (index === appState.active) row.classList.add('active');

    row.innerHTML = `
      <span class="row-number">${String(index + 1).padStart(2, '0')}</span>
      <span class="row-name"></span>
      <button class="remove-row" type="button" aria-label="Удалить имя">X</button>
    `;

    $('.row-name', row).textContent = person;
    fragment.appendChild(row);
  });

  ui.roster.replaceChildren(fragment);
}

function insertCurrentName() {
  const raw = ui.input.value;

  if (!raw.trim()) {
    notify('Введите имя');
    return;
  }

  if (containsGarbage(raw)) {
    ui.dialog.showModal();
    return;
  }

  const prepared = formatName(raw);

  if (isAlreadyStored(prepared)) {
    alert('Уже есть в списке');
    return;
  }

  rememberRoster();
  appState.people.push(prepared);
  appState.active = appState.people.length - 1;
  ui.input.value = '';
  drawRoster();
  notify(`Добавлено: ${prepared}`);
}

function removeAt(index) {
  rememberRoster();
  appState.people.splice(index, 1);

  if (appState.people.length === 0) {
    appState.active = null;
  } else if (appState.active === null || appState.active >= appState.people.length) {
    appState.active = appState.people.length - 1;
  }

  drawRoster();
  notify('Строка удалена');
}

function rollbackRoster() {
  const previous = appState.snapshots.pop();

  if (!previous) {
    notify('Нет действий для отмены');
    return;
  }

  appState.people = previous;
  appState.active = null;
  drawRoster();
  notify('Последнее изменение отменено');
}

function moveSelection(direction) {
  const count = appState.people.length;
  if (!count) return;

  if (appState.active === null) {
    appState.active = direction > 0 ? 0 : count - 1;
  } else {
    appState.active = (appState.active + direction + count) % count;
  }

  drawRoster();
}

ui.roster.addEventListener('click', event => {
  const row = event.target.closest('.roster-item');
  if (!row) return;

  const index = Number(row.dataset.index);

  if (event.target.closest('.remove-row')) {
    removeAt(index);
    return;
  }

  appState.active = index;
  drawRoster();
});

ui.add.addEventListener('click', insertCurrentName);
ui.input.addEventListener('keydown', event => {
  if (event.key === 'Enter') insertCurrentName();
});

ui.scan.addEventListener('click', () => {
  const value = ui.input.value;

  if (!value.trim()) {
    notify('Сначала введите текст');
  } else if (containsGarbage(value)) {
    ui.dialog.showModal();
  } else {
    notify('Лишних символов нет');
  }
});

ui.clean.addEventListener('click', () => {
  ui.input.value = stripGarbage(ui.input.value);
  notify('Лишние символы удалены');
  setTimeout(() => ui.input.focus(), 0);
});

ui.undo.addEventListener('click', rollbackRoster);

document.addEventListener('keydown', event => {
  const ctrlZ = (event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'z';

  if (ctrlZ) {
    event.preventDefault();
    rollbackRoster();
    return;
  }

  if (event.key === 'ArrowDown') {
    event.preventDefault();
    moveSelection(1);
  }

  if (event.key === 'ArrowUp') {
    event.preventDefault();
    moveSelection(-1);
  }
});

drawRoster();
