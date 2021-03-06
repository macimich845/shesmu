import { actionRender } from "./actions.js";

export function fetchConstant(name, element) {
  element.className = "busy";
  element.innerText = "Fetching...";
  fetch("/constant", {
    body: JSON.stringify(name),
    method: "POST"
  })
    .then(response => {
      element.innerText = "🔄 Refresh";
      element.className = "load";
      if (response.ok) {
        return Promise.resolve(response);
      } else {
        return Promise.reject(new Error("Failed to load"));
      }
    })
    .then(response => response.json())
    .then(data => {
      if (data.hasOwnProperty("value")) {
        element.nextElementSibling.innerText = data.value;
        element.nextElementSibling.className = "data";
      } else {
        element.nextElementSibling.innerText = data.error;
        element.nextElementSibling.className = "error";
      }
      element.innerText = "▶ Get";
      element.className = "load";
    })
    .catch(function(error) {
      element.nextElementSibling.innerText = error.message;
      element.nextElementSibling.className = "error";
    });
}

export function runFunction(name, element, parameterParser) {
  let parameters = [];
  let paramsOk = true;
  for (let parameter = 0; parameter < parameterParser.length; parameter++) {
    paramsOk &= parser.parse(
      document.getElementById(`${name}$${parameter}`).value,
      parameterParser[parameter],
      x => parameters.push(x),
      message => {
        element.nextElementSibling.innerText = `Argument ${parameter}: ${message}`;
        element.nextElementSibling.className = "error";
      }
    );
  }
  if (!paramsOk) {
    return;
  }
  element.className = "busy";
  element.innerText = "Running...";
  element.nextElementSibling.innerText = "";
  element.nextElementSibling.className = "";
  fetch("/function", {
    body: JSON.stringify({ name: name, args: parameters }),
    method: "POST"
  })
    .then(response => {
      element.innerText = "▶ Run";
      element.className = "load";
      if (response.ok) {
        return Promise.resolve(response);
      } else {
        return Promise.reject(new Error("Failed to load"));
      }
    })
    .then(response => response.json())
    .then(data => {
      if (data.hasOwnProperty("value")) {
        element.nextElementSibling.innerText = data.value;
        element.nextElementSibling.className = "data";
      } else {
        element.innerText = data.error;
        element.className = "error";
      }
    })
    .catch(function(error) {
      element.innerText = error.message;
      element.className = "error";
    });
}

export function prettyType() {
  const element = document.getElementById("prettyType");
  element.className = "busy";
  element.innerText = "Prettying...";
  fetch("/type", {
    body: JSON.stringify(document.getElementById("uglySignature").value),
    method: "POST"
  })
    .then(response => {
      if (response.ok) {
        return Promise.resolve(response);
      } else if (response.code === 400) {
        return Promise.reject(new Error("Invalid type signature."));
      } else {
        return Promise.reject(new Error("Failed to load"));
      }
    })
    .then(response => response.json())
    .then(data => {
      element.innerText = data;
      element.className = "data";
    })
    .catch(function(error) {
      element.innerText = error.message;
      element.className = "error";
    });
}

export const parser = {
  _: function(input) {
    return { good: false, input: input, error: "Cannot parse bad type." };
  },
  a: function(innerType) {
    return input => {
      const output = [];
      for (;;) {
        let match = input.match(output.length == 0 ? /^\s*\[/ : /^\s*([\],])/);
        if (!match) {
          return {
            good: false,
            input: input,
            error:
              output.length == 0
                ? "Expected [ in list."
                : "Expected ] or , for list."
          };
        }
        if (match[1] == "]") {
          return {
            good: true,
            input: input.substring(match[0].length),
            output: output
          };
        }
        const state = innerType(input.substring(match[0].length));
        if (state.good) {
          output.push(state.output);
          input = state.input;
        } else {
          return state;
        }
      }
    };
  },
  b: function(input) {
    let match = input.match(/^\s*([Tt]rue|[Ff]alse)/);
    if (match) {
      return {
        good: true,
        input: input.substring(match[0].length),
        output: match[1].toLowerCase() == "true"
      };
    } else {
      return { good: false, input: input, error: "Expected boolean." };
    }
  },
  d: function(input) {
    let match = input.match(/^\s*EpochSecond\s+(\d*)/);
    if (match) {
      return {
        good: true,
        input: input.substring(match[0].length),
        output: parseInt(match[1]) * 1000
      };
    }
    match = input.match(/^\s*EpochMilli\s+(\d*)/);
    if (match) {
      return {
        good: true,
        input: input.substring(match[0].length),
        output: parseInt(match[1])
      };
    }
    match = input.match(
      /^\s*Date\s+(\d{4}-\d{2}-\d{2}(T\d{2}:\d{2}:\d{2}(Z|[+-]\d{2}))?)/
    );
    if (match) {
      return {
        good: true,
        input: input.substring(match[0].length),
        output: new Date(match[1]).getTime()
      };
    } else {
      return { good: false, input: input, error: "Expected date." };
    }
  },
  i: function(input) {
    let match = input.match(/^\s*(\d*)/);
    if (match) {
      return {
        good: true,
        input: input.substring(match[0].length),
        output: parseInt(match[1])
      };
    } else {
      return { good: false, input: input, error: "Expected integer." };
    }
  },
  s: function(input) {
    let match = input.match(/^\s*"(([^"\\]|\\")*)"/);
    if (match) {
      return {
        good: true,
        input: input.substring(match[0].length),
        output: match[1].replace('\\"', '"')
      };
    } else {
      return { good: false, input: input, error: "Expected string." };
    }
  },
  t: function(innerTypes) {
    return input => {
      const output = [];
      for (let i = 0; i < innerTypes.length; i++) {
        let match = input.match(i == 0 ? /^\s*{/ : /^\s*,/);
        if (!match) {
          return {
            good: false,
            input: input,
            error: i == 0 ? "Expected { for tuple." : "Expected , for tuple."
          };
        }
        const state = innerTypes[i](input.substring(match[0].length));
        if (state.good) {
          output.push(state.output);
          input = state.input;
        } else {
          return state;
        }
      }
      let closeMatch = input.match(/^\s*}/);
      if (closeMatch) {
        return {
          good: true,
          input: input.substring(closeMatch[0].length),
          output: output
        };
      } else {
        return { good: false, input: input, error: "Expected } in tuple." };
      }
    };
  },
  parse: function(input, parse, resultHandler, errorHandler) {
    let state = parse(input);
    if (!state.good) {
      errorHandler(state.error, input.length - state.input.length);
      return false;
    }
    if (state.input.match(/^\s*$/) == null) {
      errorHandler("Junk at end of input.", input.length - state.input.length);
      return false;
    }
    resultHandler(state.output);
    return true;
  }
};

const actionStates = [
  "FAILED",
  "INFLIGHT",
  "QUEUED",
  "SUCCEEDED",
  "THROTTLED",
  "UNKNOWN",
  "WAITING"
];
const types = [];
const locations = [];

export function clearActionStates() {
  actionStates.forEach(s => {
    document.getElementById(`include_${s}`).checked = false;
  });
}

function drawTypes() {
  const container = document.getElementById("types");
  while (container.hasChildNodes()) {
    container.removeChild(container.lastChild);
  }

  types.sort();
  types.forEach(typeName => {
    const element = document.createElement("SPAN");
    element.innerText = typeName + " ";
    const removeElement = document.createElement("SPAN");
    removeElement.innerText = "✖";
    removeElement.onclick = function() {
      removeTypeName(typeName);
    };

    element.appendChild(removeElement);
    container.appendChild(element);
  });
}

function removeTypeName(typeName) {
  const index = types.indexOf(typeName);
  if (index > -1) {
    types.splice(index, 1);
    drawTypes();
    const option = document.createElement("OPTION");
    option.text = typeName;
    document.getElementById("newType").add(option);
  }
}

export function clearTypes() {
  while (types.length) {
    types.pop();
  }
  drawTypes();
  fillNewTypeSelect();
}

function addType(typeName) {
  if (typeName && !types.includes(typeName)) {
    types.push(typeName);
    drawTypes();
  }
}

export function addTypeForm() {
  const element = document.getElementById("newType");
  addType(element.value.trim());
  element.remove(element.selectedIndex);
}

export function fillNewTypeSelect() {
  const element = document.getElementById("newType");
  while (element.length > 0) {
    element.remove(0);
  }
  for (const typeName of actionRender.keys()) {
    const option = document.createElement("OPTION");
    option.text = typeName;
    element.add(option);
  }
}

function drawLocations() {
  const container = document.getElementById("locations");
  while (container.hasChildNodes()) {
    container.removeChild(container.lastChild);
  }

  locations.sort(
    (a, b) =>
      a.file.localeCompare(b.file) ||
      (a.line || 0) - (b.line || 0) ||
      (a.column || 0) - (b.column || 0) ||
      (a.time || 0) - (b.time || 0)
  );
  locations.forEach(sourceLocation => {
    const element = document.createElement("SPAN");
    element.innerText = `${sourceLocation.file}${
      sourceLocation.line === null
        ? ""
        : ":" +
          sourceLocation.line +
          (sourceLocation.column === null ? "" : ":" + sourceLocation.column)
    } `;
    const removeElement = document.createElement("SPAN");
    removeElement.innerText = "✖";
    removeElement.onclick = function() {
      const index = locations.findIndex(
        l =>
          l.file === sourceLocation.file &&
          l.line === sourceLocation.line &&
          l.column === sourceLocation.column &&
          l.time === sourceLocation.time
      );
      if (index > -1) {
        locations.splice(index, 1);
        drawLocations();
      }
    };

    element.appendChild(removeElement);
    container.appendChild(element);
  });
}

export function clearLocations() {
  while (locations.length) {
    locations.pop();
  }
  drawLocations();
}

export function addLocationForm() {
  const element = document.getElementById("newLocation");
  const match = element.value
    .trim()
    .match(/^(.*\.(shesmu|actnow))(:(\d+)(:(\d+))?)?$/);
  if (match == null) {
    element.className = "error";
    return;
  }
  element.className = "";
  const sourceLocation = {
    file: match[1],
    line: typeof match[4] == "undefined" ? null : parseInt(match[4]),
    column: typeof match[6] == "undefined" ? null : parseInt(match[6]),
    time: null
  };
  element.value = "";
  const index = locations.findIndex(
    l =>
      l.file === sourceLocation.file &&
      l.line === sourceLocation.line &&
      l.column === sourceLocation.column &&
      l.time === sourceLocation.time
  );
  if (index == -1) {
    locations.push(sourceLocation);
    drawLocations();
  }
}

function parseEpoch(elementId) {
  const epochElement = document.getElementById(elementId);
  const epochInput = epochElement.value.trim();
  epochElement.className = null;
  if (epochInput.length == 0) {
    return null;
  }
  const result = parser.d(epochInput);
  if (result.good) {
    return result.output;
  } else {
    epochElement.className = "error";
    return null;
  }
}

function makeFilters() {
  const filters = [];
  const selectedStates = actionStates.filter(
    s => document.getElementById(`include_${s}`).checked
  );
  if (selectedStates.length) {
    filters.push({ type: "status", states: selectedStates });
  }
  for (let span of ["added", "checked"]) {
    const start = parseEpoch(`${span}Start`);
    const end = parseEpoch(`${span}End`);
    if (start !== null && end != null) {
      filters.push({ type: span, start: start, end: end });
    }
  }
  if (types.length > 0) {
    filters.push({ type: "type", types: types });
  }
  if (locations.length > 0) {
    filters.push({ type: "sourcelocation", locations: locations });
  }
  return filters;
}

function results(container, slug, body, render) {
  while (container.hasChildNodes()) {
    container.removeChild(container.lastChild);
  }
  fetch(slug, {
    body: body,
    method: "POST"
  })
    .then(response => {
      if (response.ok) {
        return Promise.resolve(response);
      } else {
        return Promise.reject(new Error("Failed to load"));
      }
    })
    .then(response => response.json())
    .then(data => render(container, data))
    .catch(function(error) {
      const element = document.createElement("SPAN");
      element.innerText = error.message;
      element.className = "error";
      container.appendChild(element);
    });
}

export function listActions() {
  const query = {
    filters: makeFilters(),
    limit: 25,
    skip: 0
  };
  nextPage(query, document.getElementById("results"));
}

export function text(t) {
  const element = document.createElement("P");
  if (t.length > 100) {
    let visible = true;
    element.title = "There's a lot to unpack here.";
    element.onclick = function() {
      visible = !visible;
      element.innerText = visible ? t : t.substring(0, 98) + "...";
    };
    element.onclick();
  } else {
    element.innerText = t;
  }
  return element;
}

export function link(url, t) {
  const element = document.createElement("A");
  element.innerText = t + " 🔗";
  element.target = "_blank";
  element.href = url;
  return element;
}

export function jsonParameters(action) {
  return Object.entries(action.parameters).map(p =>
    text(`Parameter ${p[0]} = ${JSON.stringify(p[1])}`)
  );
}

export function title(action, t) {
  const element = action.url ? link(action.url, t) : text(t);
  element.title = action.state;
  return element;
}

function defaultRenderer(action) {
  return [title(action, `Unknown Action: ${action.type}`)];
}

function nextPage(query, targetElement) {
  results(targetElement, "/query", JSON.stringify(query), (container, data) => {
    const jumble = document.createElement("DIV");
    if (data.results.length == 0) {
      jumble.innerText = "No actions found.";
    }

    data.results.forEach(action => {
      const tile = document.createElement("DIV");
      tile.className = `action state_${action.state.toLowerCase()}`;
      (actionRender.get(action.type) || defaultRenderer)(action).forEach(
        element => tile.appendChild(element)
      );
      const checkDate = new Date(action.lastChecked * 1000).toString();
      const addedDate = new Date(action.lastAdded * 1000).toString();
      const statusChangedDate = new Date(
        action.lastStatusChange * 1000
      ).toString();
      tile.appendChild(
        text(`Last Checked: ${checkDate} (${action.lastChecked})`)
      );
      tile.appendChild(text(`Last Added: ${addedDate} (${action.lastAdded})`));
      tile.appendChild(
        text(
          `Last Status Change: ${statusChangedDate} (${
            action.lastStatusChange
          })`
        )
      );
      action.locations.forEach(l => {
        const t = `Made from ${l.file}:${l.line}:${l.column}[${new Date(
          l.time
        ).toISOString()}]`;
        tile.appendChild(l.url ? link(l.url, t) : text(t));
      });
      const showHide = document.createElement("P");
      const json = document.createElement("PRE");
      json.innerText = JSON.stringify(action, null, 2);
      tile.appendChild(showHide);
      tile.appendChild(json);
      let visible = true;
      showHide.onclick = () => {
        visible = !visible;
        showHide.innerText = visible ? "⊟ JSON" : "⊞ JSON";
        json.style = visible ? "display: block" : "display: none";
      };
      showHide.onclick();
      jumble.appendChild(tile);
    });

    container.appendChild(jumble);
    if (data.total == data.results.length) {
      const size = document.createElement("DIV");
      size.innerText = `${data.total} actions.`;
      container.appendChild(size);
    } else {
      const size = document.createElement("DIV");
      size.innerText = `${data.results.length} of ${data.total} actions.`;
      container.appendChild(size);
      const pager = document.createElement("DIV");
      const numButtons = Math.ceil(data.total / query.limit);
      const current = Math.floor(query.skip / query.limit);

      let rendering = true;
      for (let i = 0; i < numButtons; i++) {
        if (
          i <= 2 ||
          i >= numButtons - 2 ||
          (i >= current - 2 && i <= current + 2)
        ) {
          rendering = true;
          const page = document.createElement("SPAN");
          const skip = i * query.limit;
          page.innerText = `${i + 1}`;
          if (skip != query.skip) {
            page.className = "load";
          }
          page.onclick = () =>
            nextPage(
              {
                filters: query.filters,
                skip: skip,
                limit: query.limit
              },
              targetElement
            );
          pager.appendChild(page);
        } else if (rendering) {
          const ellipsis = document.createElement("SPAN");
          ellipsis.innerText = "...";
          pager.appendChild(ellipsis);
          rendering = false;
        }
      }
      container.appendChild(pager);
    }
  });
}

export function queryStats() {
  getStats(makeFilters(), document.getElementById("results"));
}

function showFilterJson(filters, targetElement) {
  while (targetElement.hasChildNodes()) {
    targetElement.removeChild(targetElement.lastChild);
  }
  const pre = document.createElement("PRE");
  pre.innerText = JSON.stringify(filters, null, 2);
  targetElement.appendChild(pre);
}

export function showQuery() {
  showFilterJson(makeFilters(), document.getElementById("results"));
}

function propertyFilterMaker(name) {
  switch (name) {
    case "sourcefile":
      return f => ({ type: "sourcefile", files: [f] });
    case "sourcelocation":
      return l => ({ type: "sourcelocation", locations: [l] });
    case "status":
      return s => ({ type: "status", states: [s] });
    case "type":
      return t => ({ type: "type", types: [t] });
    default:
      return () => null;
  }
}

function nameForBin(name) {
  switch (name) {
    case "added":
      return "Time Since Action was Last Generated by an Olive";
    case "checked":
      return "Last Time Action was Last Run";
    case "statuschanged":
      return "Last Time Action's Status Last Changed";
    default:
      return name;
  }
}

function formatBin(name) {
  switch (name) {
    case "added":
    case "checked":
      return x => {
        const d = new Date(x * 1000);
        let diff = Math.ceil((new Date() - d) / 1000);
        let ago = "";
        for (let span of [
          [604800, "w"],
          [86400, "d"],
          [3600, "h"],
          [60, "m"]
        ]) {
          const chunk = Math.floor(diff / span[0]);
          if (chunk > 0) {
            ago = `${ago}${chunk}${span[1]}`;
            diff = diff % span[0];
          }
        }
        if (diff > 0 || !ago) {
          ago = `${ago}${diff}s ago`;
        }
        return [ago, `${x} ${d.toISOString()}`];
      };
    default:
      return x => [x, ""];
  }
}

function setColorIntensity(element, value, maximum) {
  element.style.backgroundColor = `hsl(260, 100%, ${Math.ceil(
    97 - (value || 0) / maximum * 20
  )}%)`;
}

function getStats(filters, targetElement) {
  results(
    targetElement,
    "/stats",
    JSON.stringify(filters),
    (container, data) => {
      if (data.length == 0) {
        container.innerText = "No statistics are available.";
      }

      const drillDown = document.createElement("DIV");
      data.forEach(stat => {
        const element = document.createElement("DIV");
        const makeClick = (clickable, filters) => {
          clickable.onclick = () => {
            while (drillDown.hasChildNodes()) {
              drillDown.removeChild(drillDown.lastChild);
            }
            const clickResult = document.createElement("DIV");
            const toolBar = document.createElement("P");
            const listButton = document.createElement("SPAN");
            listButton.className = "load";
            listButton.innerText = "🔍 List";
            toolBar.appendChild(listButton);
            const statsButton = document.createElement("SPAN");
            statsButton.className = "load";
            statsButton.innerText = "📈 Stats";
            toolBar.appendChild(statsButton);
            const jsonButton = document.createElement("SPAN");
            jsonButton.className = "load";
            jsonButton.innerText = "🛈 Show Request";
            toolBar.appendChild(jsonButton);
            listButton.onclick = () => {
              nextPage(
                {
                  filters: filters,
                  limit: 25,
                  skip: 0
                },
                clickResult
              );
            };
            statsButton.onclick = () => {
              getStats(filters, clickResult);
            };
            jsonButton.onclick = () => {
              showFilterJson(filters, clickResult);
            };
            drillDown.appendChild(toolBar);
            drillDown.appendChild(clickResult);
          };
        };
        switch (stat.type) {
          case "text":
            (() => {
              element.innerText = stat.value;
            })();
            break;
          case "table":
            (() => {
              const table = document.createElement("TABLE");
              element.appendChild(table);
              stat.table.forEach(row => {
                let prettyTitle;
                switch (row.kind) {
                  case "bin":
                    prettyTitle = x => `${x} ${nameForBin(row.type)}`;
                    break;
                  case "property":
                    prettyTitle = x => `${x} ${row.property}`;
                    break;
                  default:
                    prettyTitle = x => x;
                }
                const tr = document.createElement("TR");
                table.appendChild(tr);
                const title = document.createElement("TD");
                title.innerText = prettyTitle(row.title);
                tr.appendChild(title);
                const value = document.createElement("TD");
                if (row.kind == "bin") {
                  const values = formatBin(row.type)(row.value);
                  value.innerText = values[0];
                  value.title = values[1];
                } else {
                  value.innerText = row.value;
                }
                tr.appendChild(value);
                if (row.kind == "property") {
                  makeClick(
                    tr,
                    filters.concat([propertyFilterMaker(row.type)(row.json)])
                  );
                } else {
                  tr.onclick = () => {
                    while (drillDown.hasChildNodes()) {
                      drillDown.removeChild(drillDown.lastChild);
                    }
                  };
                }
              });
            })();
            break;
          case "crosstab":
            (() => {
              const makeColumnFilter = propertyFilterMaker(stat.column);
              const makeRowFilter = propertyFilterMaker(stat.row);

              const table = document.createElement("TABLE");
              element.appendChild(table);

              const header = document.createElement("TR");
              table.appendChild(header);

              header.appendChild(document.createElement("TH"));
              const columns = stat.columns.sort().map(col => ({
                name: col.name,
                filter: makeColumnFilter(col.value)
              }));
              for (let col of columns) {
                const currentHeader = document.createElement("TH");
                currentHeader.innerText = col.name;
                header.appendChild(currentHeader);
                makeClick(currentHeader, filters.concat([col.filter]));
              }
              const maximum = Math.max(
                1,
                Math.max(
                  ...Object.values(stat.data).map(row =>
                    Math.max(...Object.values(row))
                  )
                )
              );

              for (let rowKey of Object.keys(stat.data).sort()) {
                const rowFilter = makeRowFilter(stat.rows[rowKey]);
                const currentRow = document.createElement("TR");
                table.appendChild(currentRow);

                const currentHeader = document.createElement("TH");
                currentHeader.innerText = rowKey;
                currentRow.appendChild(currentHeader);
                makeClick(currentHeader, filters.concat([rowFilter]));

                for (let col of columns) {
                  const currentValue = document.createElement("TD");
                  currentValue.innerText = stat.data[rowKey][col.name] || "0";
                  currentRow.appendChild(currentValue);
                  setColorIntensity(
                    currentValue,
                    stat.data[rowKey][col.name],
                    maximum
                  );
                  makeClick(
                    currentValue,
                    filters.concat([col.filter, rowFilter])
                  );
                }
              }
            })();
            break;

          case "histogram":
            (() => {
              const section = document.createElement("H1");
              section.innerText = nameForBin(stat.bin);
              element.appendChild(section);
              const maximum = Math.max(1, Math.max(...stat.counts));
              const table = document.createElement("TABLE");
              element.appendChild(table);
              const header = document.createElement("TR");
              table.appendChild(header);
              const startHeader = document.createElement("TH");
              startHeader.innerText = "≥";
              header.appendChild(startHeader);
              const endHeader = document.createElement("TH");
              endHeader.innerText = "<";
              header.appendChild(endHeader);
              const valueHeader = document.createElement("TH");
              valueHeader.innerText = "Actions";
              header.appendChild(valueHeader);

              const formattedBoundaries = stat.boundaries.map(
                formatBin(stat.bin)
              );

              for (let i = 0; i < stat.counts.length; i++) {
                const row = document.createElement("TR");
                table.appendChild(row);
                const start = document.createElement("TH");
                start.innerText = formattedBoundaries[i][0];
                start.title = formattedBoundaries[i][1];
                row.appendChild(start);
                makeClick(
                  start,
                  filters.concat([
                    {
                      type: stat.bin,
                      start: stat.boundaries[i],
                      end: null
                    }
                  ])
                );

                const end = document.createElement("TH");
                end.innerText = formattedBoundaries[i + 1][0];
                end.title = formattedBoundaries[i + 1][1];
                row.appendChild(end);
                makeClick(
                  end,
                  filters.concat([
                    {
                      type: stat.bin,
                      start: null,
                      end: stat.boundaries[i + 1]
                    }
                  ])
                );

                const value = document.createElement("TD");
                value.innerText = stat.counts[i];
                row.appendChild(value);
                makeClick(
                  value,
                  filters.concat([
                    {
                      type: stat.bin,
                      start: stat.boundaries[i],
                      end: stat.boundaries[i + 1]
                    }
                  ])
                );
                setColorIntensity(value, stat.counts[i], maximum);
              }
            })();
            break;

          default:
            element.innerText = `Unknown stat type: ${stat.type}`;
        }
        container.appendChild(element);
      });
      container.appendChild(drillDown);
    }
  );
}
