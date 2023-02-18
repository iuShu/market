const activity = '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-activity" viewBox="0 0 16 16">' +
    '<path fill-rule="evenodd" d="M6 2a.5.5 0 0 1 .47.33L10 12.036l1.53-4.208A.5.5 0 0 1 12 7.5h3.5a.5.5 0 0 1 0 1h-3.15l-1.88 5.17a.5.5 0 0 1-.94 0L6 3.964 4.47 8.171A.5.5 0 0 1 4 8.5H.5a.5.5 0 0 1 0-1h3.15l1.88-5.17A.5.5 0 0 1 6 2Z"/>' +
    '</svg>'
const runningIcon = '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-rocket-takeoff" viewBox="0 0 16 16">' +
    '<path d="M9.752 6.193c.599.6 1.73.437 2.528-.362.798-.799.96-1.932.362-2.531-.599-.6-1.73-.438-2.528.361-.798.8-.96 1.933-.362 2.532Z"/>' +
    '<path d="M15.811 3.312c-.363 1.534-1.334 3.626-3.64 6.218l-.24 2.408a2.56 2.56 0 0 1-.732 1.526L8.817 15.85a.51.51 0 0 1-.867-.434l.27-1.899c.04-.28-.013-.593-.131-.956a9.42 9.42 0 0 0-.249-.657l-.082-.202c-.815-.197-1.578-.662-2.191-1.277-.614-.615-1.079-1.379-1.275-2.195l-.203-.083a9.556 9.556 0 0 0-.655-.248c-.363-.119-.675-.172-.955-.132l-1.896.27A.51.51 0 0 1 .15 7.17l2.382-2.386c.41-.41.947-.67 1.524-.734h.006l2.4-.238C9.005 1.55 11.087.582 12.623.208c.89-.217 1.59-.232 2.08-.188.244.023.435.06.57.093.067.017.12.033.16.045.184.06.279.13.351.295l.029.073a3.475 3.475 0 0 1 .157.721c.055.485.051 1.178-.159 2.065Zm-4.828 7.475.04-.04-.107 1.081a1.536 1.536 0 0 1-.44.913l-1.298 1.3.054-.38c.072-.506-.034-.993-.172-1.418a8.548 8.548 0 0 0-.164-.45c.738-.065 1.462-.38 2.087-1.006ZM5.205 5c-.625.626-.94 1.351-1.004 2.09a8.497 8.497 0 0 0-.45-.164c-.424-.138-.91-.244-1.416-.172l-.38.054 1.3-1.3c.245-.246.566-.401.91-.44l1.08-.107-.04.039Zm9.406-3.961c-.38-.034-.967-.027-1.746.163-1.558.38-3.917 1.496-6.937 4.521-.62.62-.799 1.34-.687 2.051.107.676.483 1.362 1.048 1.928.564.565 1.25.941 1.924 1.049.71.112 1.429-.067 2.048-.688 3.079-3.083 4.192-5.444 4.556-6.987.183-.771.18-1.345.138-1.713a2.835 2.835 0 0 0-.045-.283 3.078 3.078 0 0 0-.3-.041Z"/>' +
    '<path d="M7.009 12.139a7.632 7.632 0 0 1-1.804-1.352A7.568 7.568 0 0 1 3.794 8.86c-1.102.992-1.965 5.054-1.839 5.18.125.126 3.936-.896 5.054-1.902Z"/>' +
    '</svg>'
const short_icon = '<svg xmlns="http://www.w3.org/2000/svg" color="red" width="16" height="16" fill="currentColor" class="bi bi-graph-down-arrow" viewBox="0 0 16 16">\n' +
    '  <path fill-rule="evenodd" d="M0 0h1v15h15v1H0V0Zm10 11.5a.5.5 0 0 0 .5.5h4a.5.5 0 0 0 .5-.5v-4a.5.5 0 0 0-1 0v2.6l-3.613-4.417a.5.5 0 0 0-.74-.037L7.06 8.233 3.404 3.206a.5.5 0 0 0-.808.588l4 5.5a.5.5 0 0 0 .758.06l2.609-2.61L13.445 11H10.5a.5.5 0 0 0-.5.5Z"/>\n' +
    '</svg>'
const long_icon = '<svg xmlns="http://www.w3.org/2000/svg" color="green" width="16" height="16" fill="currentColor" class="bi bi-graph-up-arrow" viewBox="0 0 16 16">\n' +
    '  <path fill-rule="evenodd" d="M0 0h1v15h15v1H0V0Zm10 3.5a.5.5 0 0 1 .5-.5h4a.5.5 0 0 1 .5.5v4a.5.5 0 0 1-1 0V4.9l-3.613 4.417a.5.5 0 0 1-.74.037L7.06 6.767l-3.656 5.027a.5.5 0 0 1-.808-.588l4-5.5a.5.5 0 0 1 .758-.06l2.609 2.61L13.445 4H10.5a.5.5 0 0 1-.5-.5Z"/>\n' +
    '</svg>'
const total_icon = '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-journal-text" viewBox="0 0 16 16">\n' +
    '  <path d="M5 10.5a.5.5 0 0 1 .5-.5h2a.5.5 0 0 1 0 1h-2a.5.5 0 0 1-.5-.5zm0-2a.5.5 0 0 1 .5-.5h5a.5.5 0 0 1 0 1h-5a.5.5 0 0 1-.5-.5zm0-2a.5.5 0 0 1 .5-.5h5a.5.5 0 0 1 0 1h-5a.5.5 0 0 1-.5-.5zm0-2a.5.5 0 0 1 .5-.5h5a.5.5 0 0 1 0 1h-5a.5.5 0 0 1-.5-.5z"/>\n' +
    '  <path d="M3 0h10a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2v-1h1v1a1 1 0 0 0 1 1h10a1 1 0 0 0 1-1V2a1 1 0 0 0-1-1H3a1 1 0 0 0-1 1v1H1V2a2 2 0 0 1 2-2z"/>\n' +
    '  <path d="M1 5v-.5a.5.5 0 0 1 1 0V5h.5a.5.5 0 0 1 0 1h-2a.5.5 0 0 1 0-1H1zm0 3v-.5a.5.5 0 0 1 1 0V8h.5a.5.5 0 0 1 0 1h-2a.5.5 0 0 1 0-1H1zm0 3v-.5a.5.5 0 0 1 1 0v.5h.5a.5.5 0 0 1 0 1h-2a.5.5 0 0 1 0-1H1z"/>\n' +
    '</svg>'
const sun_icon = '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-sun" viewBox="0 0 16 16">\n' +
    '  <path d="M8 11a3 3 0 1 1 0-6 3 3 0 0 1 0 6zm0 1a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM8 0a.5.5 0 0 1 .5.5v2a.5.5 0 0 1-1 0v-2A.5.5 0 0 1 8 0zm0 13a.5.5 0 0 1 .5.5v2a.5.5 0 0 1-1 0v-2A.5.5 0 0 1 8 13zm8-5a.5.5 0 0 1-.5.5h-2a.5.5 0 0 1 0-1h2a.5.5 0 0 1 .5.5zM3 8a.5.5 0 0 1-.5.5h-2a.5.5 0 0 1 0-1h2A.5.5 0 0 1 3 8zm10.657-5.657a.5.5 0 0 1 0 .707l-1.414 1.415a.5.5 0 1 1-.707-.708l1.414-1.414a.5.5 0 0 1 .707 0zm-9.193 9.193a.5.5 0 0 1 0 .707L3.05 13.657a.5.5 0 0 1-.707-.707l1.414-1.414a.5.5 0 0 1 .707 0zm9.193 2.121a.5.5 0 0 1-.707 0l-1.414-1.414a.5.5 0 0 1 .707-.707l1.414 1.414a.5.5 0 0 1 0 .707zM4.464 4.465a.5.5 0 0 1-.707 0L2.343 3.05a.5.5 0 1 1 .707-.707l1.414 1.414a.5.5 0 0 1 0 .708z"/>\n' +
    '</svg>'
const moon_icon = '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-moon" viewBox="0 0 16 16">\n' +
    '  <path d="M6 .278a.768.768 0 0 1 .08.858 7.208 7.208 0 0 0-.878 3.46c0 4.021 3.278 7.277 7.318 7.277.527 0 1.04-.055 1.533-.16a.787.787 0 0 1 .81.316.733.733 0 0 1-.031.893A8.349 8.349 0 0 1 8.344 16C3.734 16 0 12.286 0 7.71 0 4.266 2.114 1.312 5.124.06A.752.752 0 0 1 6 .278zM4.858 1.311A7.269 7.269 0 0 0 1.025 7.71c0 4.02 3.279 7.276 7.319 7.276a7.316 7.316 0 0 0 5.205-2.162c-.337.042-.68.063-1.029.063-4.61 0-8.343-3.714-8.343-8.29 0-1.167.242-2.278.681-3.286z"/>\n' +
    '</svg>'
const endIcon = '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-check-circle-fill" viewBox="0 0 16 16" color="blue">\n' +
    '<path d="M16 8A8 8 0 1 1 0 8a8 8 0 0 1 16 0zm-3.97-3.03a.75.75 0 0 0-1.08.022L7.477 9.417 5.384 7.323a.75.75 0 0 0-1.06 1.06L6.97 11.03a.75.75 0 0 0 1.079-.02l3.992-4.99a.75.75 0 0 0-.01-1.05z"></path>\n' +
    '</svg>'

const ttl = '<div class="row" style="width: 100%">' +
    '<div class="col-1"><span class="badge rounded-pill text-bg-{{badge}}">{{side}}</span></div>' +
    '<div class="col-4">{{start}} ~ {{end}} {{duration}}</div>' +
    '<div class="col-1">{{ttlSz}}</div>' +
    '<div class="col-3 text-truncate">{{startPx}} ' + activity + ' {{endPx}} ({{percent}}%)</div>' +
    '<div class="col-2 text-truncate" style="color:{{status}};">{{pnl}}</div></div>'
const detail = '<a href="#" class="list-group-item list-group-item-action" onclick="return false">{{detail}}</a>'
const innerDetail = '<div class="row" style="width: 100%">' +
    '<div class="col-2">{{time}}</div>' +
    '<div class="col-1">{{sz}}</div>' +
    '<div class="col-2 text-truncate">{{px}}</div>' +
    '<div class="col-2 text-truncate">' +
    '   <span style="color: green">{{tpPx}}</span> <span style="color: red">{{slPx}}</span>' +
    '</div>' +
    '<div class="col-1">{{cost}}</div>' +
    '<div class="col-1">{{open_fee}}</div>' +
    '<div class="col-1">{{endIcon}}</div></div>'
const accordion = '<div class="accordion mt-3">{{body}}</div>'
const headCard = '<div class="accordion-item shadow">' +
    '<h2 class="accordion-header" id="heading6">' +
    '<button class="accordion-button collapsed" type="button"  data-bs-toggle="collapse" data-bs-target="#collapse" aria-expanded="false" aria-controls="collapse" style="background-color: #e9ecef">' +
    '<div class="row" style="width: 100%;">' +
    '<div class="col-2 align-self-center">{{date}}</div>' +
    '<div class="col-4">' +
    '   <span class="badge rounded-pill text-bg-light">' + total_icon + ' {{ttl}}</span>' +
    '   <span class="badge rounded-pill text-bg-light ms-3">' + long_icon + ' {{long}}</span>' +
    '   <span class="badge rounded-pill text-bg-light ms-3">' + short_icon + ' {{short}}</span>' +
    '   <span class="ms-3" style="color: #c0c0c1">|</span>' +
    '   <span class="badge rounded-pill text-bg-light ms-3" title="Day 8:00 ~ 21:00">' + sun_icon + ' {{day}}</span>' +
    '   <span class="badge rounded-pill text-bg-dark ms-3" title="Night 21:00 ~ 8:00">' + moon_icon + ' {{night}}</span>' +
    '</div>' +
    // '<div class="col-2"><b class="pe-2" style="color: {{color}}">PNL {{pnl}}</b>{{end}}</div>' +
    '<div class="col-2"><span class="badge rounded-pill text-bg-{{color}} ms-3" style="font-size: 14px">PNL {{pnl}}</span></div>' +
    // '<div class="col-2">' +
    // '   <span class="badge rounded-pill text-bg-light" title="Day 8:00 ~ 21:00">' + sun_icon + ' {{day}}</span>' +
    // '   <span class="badge rounded-pill text-bg-dark ms-3" title="Night 21:00 ~ 8:00">' + moon_icon + ' {{night}}</span>' +
    // '</div>' +
    '</div></button></h2></div>'
const card = '<div class="accordion-item shadow">' +
    '<h2 class="accordion-header" id="{{headingId}}">' +
    '<button class="accordion-button collapsed" type="button" data-bs-toggle="collapse" data-bs-target="#{{collapseId}}" aria-expanded="false" aria-controls="{{collapseId}}">{{title}}</button>' +
    '</h2>' +
    '<div id="{{collapseId}}" class="accordion-collapse collapse" aria-labelledby="{{headingId}}" data-bs-parent="#list">' +
    '<div class="accordion-body"><div class="list-group">{{content}}</div></div></div></div>'

let collapseIdx = 0
let headingIdx = 0
let lever = 0
const precision = 10000
let base_size = 1, unit = 100
const maker_fee = 0.0005, taker_fee = 0.0002

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

function createDetail(o, end) {
    const inner = innerDetail.replace('{{time}}', o.time.substring(11, 19))
        .replace('{{sz}}', o.sz + '')
        .replace('{{px}}', o.px)
        .replace('{{tpPx}}', o.tp === '' ? '' : parseFloat(o.tp).toFixed(4))
        .replace('{{slPx}}', o.sl === '' ? '' : parseFloat(o.sl).toFixed(4))
        .replace('{{cost}}', end ? '' : o.cost)
        .replace('{{open_fee}}', end ? '' : o.open_fee)
        .replace('{{endIcon}}', end ? endIcon : '')
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
        .replace('{{percent}}', calc_percent(mo.startPx, mo.endPx, mo.side))
        .replace('{{status}}', pnl > 0.0 ? 'green' : 'red')
        .replace('{{pnl}}',  pnl + '')
    const details = []
    for (let o of mo.orders)
        details.push(createDetail(o, mo.orders.length > 1 && mo.ttlSz === o.sz && mo.orders[mo.orders.length-1] === o))
    return card.replace('{{title}}', title)
        .replace('{{content}}', details.join(''))
        .replaceAll('{{collapseId}}', 'collapse' + collapseIdx.toString())
        .replaceAll('{{headingId}}', 'heading' + headingIdx.toString())
}

function createHeadCard(dmo) {
    let long = 0, short = 0, pnl = 0, date = '', end = '', day = 0, night = 0
    for (let mo of dmo) {
        if (date === '')
            date = mo.start.substring(0, 10)
        if (mo.side === 'long')
            long++
        if (mo.side === 'short')
            short++

        const hour = parseInt(mo.start.substring(11, 13))
        if (hour >= 8 && hour < 21)
            day++
        else
            night++

        const prev = pnl
        if (mo.pnl !== '')
            pnl += Math.round(parseFloat(mo.pnl) * precision)
        if (pnl === prev)
            end = runningIcon
    }
    return headCard.replace('{{date}}', date)
        .replace('{{ttl}}', (long + short) + '')
        .replace('{{long}}', long + '')
        .replace('{{short}}', short + '')
        .replace('{{color}}', pnl > 0 ? 'success' : 'danger')
        .replace('{{pnl}}', pnl / precision + '')
        .replace('{{day}}', day + '')
        .replace('{{night}}', night + '')
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

    // accordions.unshift('<hr/>')
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
    this.cost = 0
    this.open_fee = 0
    this.close_fee = 0
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

const calc_percent = function (start, end, side) {
    if (end === '?') return ''
    start *= precision
    end *= precision
    let t = side === 'long' ? (end - start) : (start - end)
    return (t / start * 100).toFixed(2)
}

const calc_cost = function (order) {
    const cost = parseFloat(order.px) * precision * order.sz / lever / precision / unit
    return cost.toFixed(4)
}

const calc_fee = function (order, rate) {
    const fee = parseFloat(order.px) * precision * order.sz * rate / lever / precision / unit
    return fee.toFixed(4)
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

        if (line.indexOf('close by take profit at') > -1 || line.indexOf('order failed this round at') > -1) {    // shadow
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
        if (line.indexOf('Tracker: filled data') > -1) {    // shadow
            const matcher = fillRegex.exec(line);
            if (matcher === null)
                throw new Error('match filled line error ' + line)
            order.px = matcher[2]
            order.sz = parseInt(matcher[1])
            order.side = matcher[4]
            order.time = line.substring(0, 23)
            martinOrder.orders.push(order)
            if (order.sz === base_size) {
                martinOrder.start = order.time
                martinOrder.side = order.side
                martinOrder.startPx = order.px
            }
            order.open_fee = calc_fee(order, maker_fee)
            order.cost = calc_cost(order)
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
            if (order.sz === base_size) {
                martinOrder.start = order.time
                martinOrder.side = order.side
                martinOrder.startPx = order.px
            }
            order.open_fee = calc_fee(order, maker_fee)
            order.cost = calc_cost(order)
            order = new Order()
        }
        if (line.indexOf('cost') > -1) {
            const matcher = costRegex.exec(line);
            if (matcher === null)
                throw new Error('match cost line error ' + line)
            martinOrder.cost = matcher[1]
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

    console.log('lever:', lever, ', got', martinOrders.length, 'batches')
    $('#lever').val(lever)
    displayAccordion(martinOrders)
    $('.accordion:first')[0].classList.add('mb-5')
}

const analysis = function () {
    base_size = $('#base-size').val(), unit = $('#base-unit').val()
    if (base_size.length < 1) {
        alert('please input base size')
        return
    }
    if (unit.length < 1) {
        alert('please input base unit')
        return
    }
    base_size = parseInt(base_size)
    unit = parseInt(unit)

    const file = $('#file')[0].files[0]
    if (!file.name.endsWith('.log')) {
        alert('not support file format')
        return
    }
    const fileReader = new FileReader()
    fileReader.onload = doAnalysis
    fileReader.readAsText(file, 'utf-8')
};
