const activity = '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-activity" viewBox="0 0 16 16">' +
    '<path fill-rule="evenodd" d="M6 2a.5.5 0 0 1 .47.33L10 12.036l1.53-4.208A.5.5 0 0 1 12 7.5h3.5a.5.5 0 0 1 0 1h-3.15l-1.88 5.17a.5.5 0 0 1-.94 0L6 3.964 4.47 8.171A.5.5 0 0 1 4 8.5H.5a.5.5 0 0 1 0-1h3.15l1.88-5.17A.5.5 0 0 1 6 2Z"/>' +
    '</svg>'
const runningIcon = '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-rocket-takeoff" viewBox="0 0 16 16">' +
    '<path d="M9.752 6.193c.599.6 1.73.437 2.528-.362.798-.799.96-1.932.362-2.531-.599-.6-1.73-.438-2.528.361-.798.8-.96 1.933-.362 2.532Z"/>' +
    '<path d="M15.811 3.312c-.363 1.534-1.334 3.626-3.64 6.218l-.24 2.408a2.56 2.56 0 0 1-.732 1.526L8.817 15.85a.51.51 0 0 1-.867-.434l.27-1.899c.04-.28-.013-.593-.131-.956a9.42 9.42 0 0 0-.249-.657l-.082-.202c-.815-.197-1.578-.662-2.191-1.277-.614-.615-1.079-1.379-1.275-2.195l-.203-.083a9.556 9.556 0 0 0-.655-.248c-.363-.119-.675-.172-.955-.132l-1.896.27A.51.51 0 0 1 .15 7.17l2.382-2.386c.41-.41.947-.67 1.524-.734h.006l2.4-.238C9.005 1.55 11.087.582 12.623.208c.89-.217 1.59-.232 2.08-.188.244.023.435.06.57.093.067.017.12.033.16.045.184.06.279.13.351.295l.029.073a3.475 3.475 0 0 1 .157.721c.055.485.051 1.178-.159 2.065Zm-4.828 7.475.04-.04-.107 1.081a1.536 1.536 0 0 1-.44.913l-1.298 1.3.054-.38c.072-.506-.034-.993-.172-1.418a8.548 8.548 0 0 0-.164-.45c.738-.065 1.462-.38 2.087-1.006ZM5.205 5c-.625.626-.94 1.351-1.004 2.09a8.497 8.497 0 0 0-.45-.164c-.424-.138-.91-.244-1.416-.172l-.38.054 1.3-1.3c.245-.246.566-.401.91-.44l1.08-.107-.04.039Zm9.406-3.961c-.38-.034-.967-.027-1.746.163-1.558.38-3.917 1.496-6.937 4.521-.62.62-.799 1.34-.687 2.051.107.676.483 1.362 1.048 1.928.564.565 1.25.941 1.924 1.049.71.112 1.429-.067 2.048-.688 3.079-3.083 4.192-5.444 4.556-6.987.183-.771.18-1.345.138-1.713a2.835 2.835 0 0 0-.045-.283 3.078 3.078 0 0 0-.3-.041Z"/>' +
    '<path d="M7.009 12.139a7.632 7.632 0 0 1-1.804-1.352A7.568 7.568 0 0 1 3.794 8.86c-1.102.992-1.965 5.054-1.839 5.18.125.126 3.936-.896 5.054-1.902Z"/>' +
    '</svg>'
const endIcon = '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-check-circle-fill" viewBox="0 0 16 16">\n' +
    '<path d="M16 8A8 8 0 1 1 0 8a8 8 0 0 1 16 0zm-3.97-3.03a.75.75 0 0 0-1.08.022L7.477 9.417 5.384 7.323a.75.75 0 0 0-1.06 1.06L6.97 11.03a.75.75 0 0 0 1.079-.02l3.992-4.99a.75.75 0 0 0-.01-1.05z"></path>\n' +
    '</svg>'
const ttl = '<div class="row" style="width: 100%">' +
    '<div class="col-1"><span class="badge rounded-pill text-bg-{{badge}}">{{side}}</span></div>' +
    '<div class="col-4">{{start}} ~ {{end}} {{duration}}</div>' +
    '<div class="col-1">{{ttlSz}}</div>' +
    '<div class="col-2 text-truncate">{{startPx}} ' + activity + ' {{endPx}}</div>' +
    '<div class="col-2 text-truncate">{{pnl}}</div></div>'
const detail = '<a href="#" class="list-group-item list-group-item-action" onclick="return false">{{detail}}</a>'
const innerDetail = '<div class="row" style="width: 100%">' +
    '<div class="col-2">{{time}}</div>' +
    '<div class="col-1">{{sz}}</div>' +
    '<div class="col-2 text-truncate">{{px}}</div>' +
    '<div class="col-2 text-truncate">{{tpPx}}</div>' +
    '<div class="col-2 text-truncate">{{slPx}}</div>' +
    '<div class="col-2"></div>' +
    '<div class="col-1">{{endIcon}}</div></div>'
const accordion = '<div class="accordion mt-3">{{body}}</div>'
const headCard = '<div class="accordion-item">' +
    '<h2 class="accordion-header" id="heading6">' +
    '<button class="accordion-button collapsed text-bg-light" type="button"  data-bs-toggle="collapse" data-bs-target="#collapse" aria-expanded="false" aria-controls="collapse">' +
    '<div class="row" style="width: 100%;">' +
    '<div class="col-2">{{date}}</div>' +
    '<div class="col-1"><span>Total {{ttl}}</span></div>' +
    '<div class="col-1"><span style="color: green">Long {{long}}</span></div>' +
    '<div class="col-1"><span style="color: red">Short {{short}}</span></div>' +
    '<div class="col-2"><b class="pe-1" style="color: {{color}}">PNL {{pnl}}</b>{{end}}</div></div></button></h2></div>'
const card = '<div class="accordion-item">' +
    '<h2 class="accordion-header" id="{{headingId}}">' +
    '<button class="accordion-button collapsed" type="button" data-bs-toggle="collapse" data-bs-target="#{{collapseId}}" aria-expanded="false" aria-controls="{{collapseId}}">{{title}}</button>' +
    '</h2>' +
    '<div id="{{collapseId}}" class="accordion-collapse collapse" aria-labelledby="{{headingId}}" data-bs-parent="#list">' +
    '<div class="accordion-body"><div class="list-group">{{content}}</div></div></div></div>'

let collapseIdx = 0
let headingIdx = 0
let lever = 0
const unit = 1000
const precision = 10000

function floatNum(num) {
    return Math.round(parseFloat(num) * precision) / precision
}

function calcPnl(mo) {
    if (mo.endPx === '')
        return

    let avgPx = 0
    for (let o of mo.orders)
        avgPx += parseFloat(o.px) * precision * o.sz
    avgPx = Math.round(avgPx / mo.ttlSz)
    const endPx = parseFloat(mo.endPx) * precision
    let diffPx = mo.side === 'long' ? (endPx - avgPx) : (avgPx - endPx)
    const profitRate = diffPx / avgPx * lever
    mo.pnl = Math.round(avgPx * mo.ttlSz * profitRate / lever) / precision / unit
}

function timeDiff(start, end) {
    if (end === 'now')
        return ''

    let dateBegin = new Date(start);
    let dateEnd = new Date(end);
    let dateDiff = dateEnd.getTime() - dateBegin.getTime();
    let dayDiff = Math.floor(dateDiff / (24 * 3600 * 1000));
    let leave1 = dateDiff % (24*3600*1000)
    let hours = Math.floor(leave1 / (3600*1000))
    let leave2 = leave1 % (3600*1000)
    let minutes = Math.floor(leave2 / (60*1000))
    let leave3 = leave2 % (60*1000)
    let seconds = Math.round(leave3 / 1000)
    return '/ ' + (dayDiff > 0 ? dayDiff + 'd' : '')
        + (hours > 0 ? hours + 'h' : '')
        + (minutes > 0 ? minutes + 'm' : '')
        // + (seconds > 0 ? seconds + 's' : '')
}

function createDetail(o) {
    const inner = innerDetail.replace('{{time}}', o.time.substring(11, 19))
        .replace('{{sz}}', o.sz + '')
        .replace('{{px}}', o.px)
        .replace('{{tpPx}}', o.tp)
        .replace('{{slPx}}', o.sl)
        .replace('{{endIcon}}', o.tp === '' ? endIcon : '')
    return detail.replace('{{detail}}', inner)
}

function createCard(mo) {
    collapseIdx++
    headingIdx++
    const end = mo.end === 'now' ? mo.end : mo.end.substring(11, 19)
    const pnl = mo.pnl === '' ? '' : floatNum(mo.pnl)
    const title = ttl.replace('{{badge}}', mo.side === 'long' ? 'success' : 'danger')
        .replace('{{side}}', mo.side.toUpperCase())
        .replace('{{start}}', mo.start.substring(11, 19))
        .replace('{{end}}', end)
        .replace('{{duration}}', timeDiff(mo.start, mo.end))
        .replace('{{ttlSz}}', mo.ttlSz + '')
        .replace('{{startPx}}', mo.startPx)
        .replace('{{endPx}}', mo.endPx)
        .replace('{{pnl}}',  pnl + '')
    const details = []
    for (let o of mo.orders)
        details.push(createDetail(o))
    return card.replace('{{title}}', title)
        .replace('{{content}}', details.join(''))
        .replaceAll('{{collapseId}}', 'collapse' + collapseIdx.toString())
        .replaceAll('{{headingId}}', 'heading' + headingIdx.toString())
}

function createHeadCard(dmo) {
    let long = 0, short = 0, pnl = 0, date = '', end = ''
    for (let mo of dmo) {
        if (date === '')
            date = mo.start.substring(0, 10)
        if (mo.side === 'long')
            long++
        if (mo.side === 'short')
            short++
        const prev = pnl
        if (mo.pnl !== '')
            pnl += Math.round(parseFloat(mo.pnl) * 10000)
        if (pnl === prev)
            end = runningIcon
    }
    return headCard.replace('{{date}}', date)
        .replace('{{ttl}}', (long + short) + '')
        .replace('{{long}}', long + '')
        .replace('{{short}}', short + '')
        .replace('{{color}}', pnl > 0 ? 'green' : 'red')
        .replace('{{pnl}}', pnl / 10000 + '')
        .replace('{{end}}', end)
}

function displayAccordion(mos) {
    const accordions = []
    let cards = [], dmo = []
    let date = ''
    for (let mo of mos) {
        const d = mo.start.substring(0, 10)
        if (date === '')
            date = d
        if (date !== d) {
            cards.unshift(createHeadCard(dmo))
            accordions.push(accordion.replace('{{body}}', cards.join('')))
            cards = []
            dmo = []
            date = d
        }
        cards.push(createCard(mo))
        dmo.push(mo)
    }
    if (cards.length > 0 || accordions.length === 0) {
        cards.unshift(createHeadCard(dmo))
        accordions.push(accordion.replace('{{body}}', cards.join('')))
    }

    accordions.unshift(accordion.replace('{{body}}', createHeadCard(mos)))
    const $board = $('#board')
    $board.empty()
    $board.append(accordions.join(''))
}

const Order = function() {
    this.px = ''
    this.sz = 0
    this.side = ''
    this.time = ''
    this.algoSz = 0
    this.tp = ''
    this.sl = ''
}

const MartinOrder = function() {
    this.start = ''
    this.end = ''
    this.side = ''
    this.startPx = ''
    this.endPx = ''
    this.ttlSz = 0
    this.cost = ''
    this.pnl = ''
    this.orders = []
}

const sumSz = function(martinOrder) {
    let ttlSz = 0
    for (let o of martinOrder.orders)
        ttlSz += o.sz
    return ttlSz
}

const doAnalysis = function (event) {
    const lines = event.target.result.split('\n')
    const costRegex = /.*Initiator: \d+ cost (\w+(.\w+)?) at.*/
    const fillRegex = /.*sz=(\w+), avgPx=(\w+(.\w+)?), posSide=(\w+), accFillSz=(\w+), state=(\w+), side=(\w+).*/
    const algoRegex = /.*tp=(\w+(.\w+)?) nx=(\w+(.\w+)?) sl=(\w+(.\w+)?).*/
    const algoRegexTest = /.*tp=(\w+(.\w+)?) sl=(\w+(.\w+)?).*/
    const martinOrders = []
    let martinOrder = new MartinOrder()
    let order = new Order()
    for (let line of lines) {
        if (line.indexOf('o.i.m') < 0)
            continue

        if (lever === 0 && line.indexOf('start trading for') > -1)
            lever = parseInt(line.substring(line.length-3, line.length-1))

        if (line.indexOf('close by take profit at ') > -1) {    // shadow
            martinOrder.end = line.substring(0, 23)
            martinOrder.endPx = floatNum(line.substring(line.indexOf('at ')+3))
            martinOrder.ttlSz = sumSz(martinOrder)
            calcPnl(martinOrder)
            const endOrder = new Order()
            endOrder.side = martinOrder.side
            endOrder.sz = martinOrder.ttlSz
            endOrder.px = martinOrder.endPx
            endOrder.time = martinOrder.end
            martinOrder.orders.push(endOrder)
            martinOrders.push(martinOrder)
            // console.log(martinOrder.startPx, martinOrder.endPx, martinOrder.ttlSz)
            martinOrder = new MartinOrder()
            martinOrder.end = 'now'
            martinOrder.endPx = '?'
        }
        if (line.indexOf('cost') > -1) {
            const matcher = costRegex.exec(line);
            if (matcher === null)
                throw new Error('match cost line error ' + line)
            martinOrder.cost = matcher[1]
        }
        if (line.indexOf('Tracker: filled data') > -1) {    // shadow
            const matcher = fillRegex.exec(line);
            if (matcher === null)
                throw new Error('match filled line error ' + line)
            order.px = matcher[2]
            order.sz = parseInt(matcher[1])
            order.side = matcher[4]
            order.time = line.substring(0, 23)
            martinOrder.orders.push(order)
            if (order.sz === 10) {
                martinOrder.start = order.time
                martinOrder.side = order.side
                martinOrder.startPx = order.px
            }
            order = new Order()
        }
        if (line.indexOf('Recorder: filled order') > -1) {  // test
            const arr = line.substring(line.indexOf('filled order ') + 13).split(' ');
            if (martinOrder.start !== '' && sumSz(martinOrder) === parseInt(arr[2])) {
                martinOrder.end = line.substring(0, 23)
                martinOrder.endPx = floatNum(arr[3])
                martinOrder.ttlSz = sumSz(martinOrder)
                continue
            }
            order.px = arr[3]
            order.sz = parseInt(arr[2])
            order.side = arr[0]
            order.time = line.substring(0, 23)
            martinOrder.orders.push(order)
            if (order.sz === 10) {
                martinOrder.start = order.time
                martinOrder.side = order.side
                martinOrder.startPx = order.px
            }
            order = new Order()
        }
        if (line.indexOf('o.i.m.t.o.c.Initiator') > -1 && line.indexOf('batch order closed') > -1) {  // test
            martinOrder.pnl = line.substring(line.indexOf('balance'), line.lastIndexOf(',')).split(' ')[2]
            const endOrder = new Order()
            endOrder.side = martinOrder.side
            endOrder.sz = martinOrder.ttlSz
            endOrder.px = martinOrder.endPx
            endOrder.time = martinOrder.end
            martinOrder.orders.push(endOrder)
            martinOrders.push(martinOrder)
            martinOrder = new MartinOrder()
            martinOrder.end = 'now'
            martinOrder.endPx = '?'
        }
        if (line.indexOf('placed algo') > -1) {
            let regex = algoRegexTest, slIdx = 3
            if (line.indexOf('nx=') > -1) {
                regex = algoRegex
                slIdx = 5
            }
            const matcher = regex.exec(line);
            if (matcher === null)
                throw new Error('match algo line error ' + line)
            order.tp = floatNum(matcher[1])
            order.sl = matcher[slIdx]
        }
    }
    if (martinOrder.start !== '') {
        martinOrder.ttlSz = sumSz(martinOrder)
        martinOrders.push(martinOrder)
    }

    console.log('got', martinOrders.length, 'batches')
    displayAccordion(martinOrders)
}

const analysis = function () {
    const file = $('#file')[0].files[0]
    if (!file.name.endsWith('.log')) {
        alert('not support file format')
        return
    }
    const fileReader = new FileReader()
    fileReader.onload = doAnalysis
    fileReader.readAsText(file, 'utf-8')
};


