import secrets
from fastapi import APIRouter, Depends, HTTPException, Form, Request
from fastapi.responses import HTMLResponse, RedirectResponse
from fastapi.security import HTTPBasic, HTTPBasicCredentials
from sqlalchemy.orm import Session, joinedload

from app.config import settings
from app.database import get_db
from app.models.household import Household, HouseholdMember, MemberRole
from app.models.user import User

router = APIRouter()
security = HTTPBasic()

CSS = """
* { box-sizing: border-box; margin: 0; padding: 0; }
body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
       background: #f0f0f4; color: #1a1a1a; font-size: 14px; }
.topbar { background: #6750A4; color: #fff; padding: 16px 24px;
          display: flex; align-items: center; gap: 12px; }
.topbar h1 { font-size: 20px; font-weight: 700; }
.topbar span { opacity: .7; font-size: 13px; }
.container { max-width: 1200px; margin: 0 auto; padding: 24px; }
.stats { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
         gap: 16px; margin-bottom: 24px; }
.stat { background: #fff; border-radius: 12px; padding: 20px; text-align: center;
        box-shadow: 0 1px 4px rgba(0,0,0,.06); }
.stat .n { font-size: 32px; font-weight: 700; color: #6750A4; }
.stat .l { color: #888; font-size: 13px; margin-top: 4px; }
.section { background: #fff; border-radius: 12px; margin-bottom: 24px;
           box-shadow: 0 1px 4px rgba(0,0,0,.06); overflow: hidden; }
.section-header { padding: 16px 20px; border-bottom: 1px solid #f0f0f4;
                  font-weight: 600; font-size: 15px; display: flex;
                  justify-content: space-between; align-items: center; }
table { width: 100%; border-collapse: collapse; }
th { background: #fafafa; padding: 10px 16px; text-align: left;
     font-weight: 600; color: #555; font-size: 12px; text-transform: uppercase;
     letter-spacing: .5px; border-bottom: 1px solid #eee; }
td { padding: 10px 16px; border-bottom: 1px solid #f5f5f5; vertical-align: middle; }
tr:last-child td { border-bottom: none; }
tr:hover td { background: #fafafa; }
.badge { display: inline-block; padding: 2px 8px; border-radius: 100px;
         font-size: 11px; font-weight: 600; }
.badge-owner { background: #EAD8FC; color: #6750A4; }
.badge-member { background: #E8F5E9; color: #2E7D32; }
.btn { display: inline-block; padding: 6px 14px; border-radius: 8px; border: none;
       cursor: pointer; font-size: 13px; font-weight: 500; text-decoration: none;
       transition: opacity .15s; }
.btn:hover { opacity: .8; }
.btn-primary { background: #6750A4; color: #fff; }
.btn-danger { background: #FDECEA; color: #C62828; }
.btn-sm { padding: 4px 10px; font-size: 12px; }
input[type=text] { border: 1px solid #ddd; border-radius: 8px; padding: 6px 10px;
                   font-size: 13px; outline: none; }
input[type=text]:focus { border-color: #6750A4; }
form { display: inline; }
.inline { display: flex; gap: 8px; align-items: center; }
.muted { color: #888; font-size: 12px; }
.expand-btn { background: none; border: none; cursor: pointer; color: #6750A4;
              font-size: 12px; font-weight: 600; padding: 0; }
details summary { cursor: pointer; color: #6750A4; font-size: 12px;
                  font-weight: 600; list-style: none; }
details summary::-webkit-details-marker { display: none; }
"""

def _verify_admin(credentials: HTTPBasicCredentials = Depends(security)):
    ok = (
        secrets.compare_digest(credentials.username.encode(), settings.admin_username.encode())
        and secrets.compare_digest(credentials.password.encode(), settings.admin_password.encode())
    )
    if not ok:
        raise HTTPException(
            status_code=401,
            detail="Falsches Passwort",
            headers={"WWW-Authenticate": "Basic"},
        )
    return credentials


def _page(title: str, body: str) -> str:
    return f"""<!DOCTYPE html>
<html lang="de">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <title>Flinkly Admin – {title}</title>
  <style>{CSS}</style>
</head>
<body>
  <div class="topbar">
    <h1>🏠 Flinkly Admin</h1>
    <span>Management-Übersicht</span>
  </div>
  <div class="container">{body}</div>
</body>
</html>"""


@router.get("", response_class=HTMLResponse)
def admin_dashboard(
    _: HTTPBasicCredentials = Depends(_verify_admin),
    db: Session = Depends(get_db),
):
    households = db.query(Household).options(
        joinedload(Household.members).joinedload(HouseholdMember.user)
    ).order_by(Household.created_at.desc()).all()

    users = db.query(User).order_by(User.created_at.desc()).all()

    total_members = sum(len(h.members) for h in households)

    # Stats
    stats_html = f"""
    <div class="stats">
      <div class="stat"><div class="n">{len(households)}</div><div class="l">Haushalte</div></div>
      <div class="stat"><div class="n">{len(users)}</div><div class="l">Nutzer</div></div>
      <div class="stat"><div class="n">{total_members}</div><div class="l">Mitgliedschaften</div></div>
    </div>"""

    # Households table
    rows = ""
    for h in households:
        member_rows = ""
        for m in sorted(h.members, key=lambda x: x.role.value):
            role_badge = f'<span class="badge badge-owner">Owner</span>' if m.role == MemberRole.OWNER else f'<span class="badge badge-member">Mitglied</span>'
            member_rows += f"""
            <tr>
              <td>{m.user.display_name}</td>
              <td>{role_badge}</td>
              <td class="muted">{m.user.total_points} Pkt &nbsp;|&nbsp; 🔥 {m.user.current_streak}</td>
              <td class="muted">{str(m.joined_at)[:10]}</td>
              <td>
                <form method="post" action="/admin/member/{m.id}/remove"
                      onsubmit="return confirm('Mitglied entfernen?')">
                  <button class="btn btn-danger btn-sm" type="submit">Entfernen</button>
                </form>
              </td>
            </tr>"""

        rows += f"""
        <tr>
          <td><strong>{h.name}</strong><br><span class="muted">ID {h.id} &nbsp;·&nbsp; erstellt {str(h.created_at)[:10]}</span></td>
          <td>{len(h.members)}</td>
          <td>
            <form method="post" action="/admin/household/{h.id}/rename" class="inline">
              <input type="text" name="name" value="{h.name}" style="width:160px">
              <button class="btn btn-primary btn-sm" type="submit">Speichern</button>
            </form>
          </td>
          <td>
            <form method="post" action="/admin/household/{h.id}/delete"
                  onsubmit="return confirm('Haushalt &quot;{h.name}&quot; wirklich löschen? Alle Räume, Aufgaben und Mitgliedschaften werden gelöscht.')">
              <button class="btn btn-danger btn-sm" type="submit">Löschen</button>
            </form>
          </td>
        </tr>
        <tr>
          <td colspan="4" style="padding: 0 0 12px 32px; background:#fafafa;">
            <details>
              <summary>▶ {len(h.members)} Mitglieder anzeigen</summary>
              <table style="margin-top:8px;">
                <tr>
                  <th>Name</th><th>Rolle</th><th>Punkte / Streak</th>
                  <th>Beigetreten</th><th>Aktion</th>
                </tr>
                {member_rows}
              </table>
            </details>
          </td>
        </tr>"""

    households_section = f"""
    <div class="section">
      <div class="section-header">Haushalte</div>
      <table>
        <tr><th>Name</th><th>Mitglieder</th><th>Umbenennen</th><th>Löschen</th></tr>
        {rows}
      </table>
    </div>"""

    # Users table
    user_rows = ""
    for u in users:
        memberships = ", ".join(m.household.name for m in u.household_memberships) or "—"
        user_rows += f"""
        <tr>
          <td><strong>{u.display_name}</strong><br>
              <span class="muted">ID {u.id}</span></td>
          <td>{u.total_points}</td>
          <td>🔥 {u.current_streak} &nbsp;(max {u.longest_streak})</td>
          <td class="muted">{memberships}</td>
          <td class="muted">{str(u.created_at)[:10]}</td>
          <td>
            <form method="post" action="/admin/user/{u.id}/rename" class="inline">
              <input type="text" name="display_name" value="{u.display_name}" style="width:130px">
              <button class="btn btn-primary btn-sm" type="submit">OK</button>
            </form>
          </td>
          <td>
            <form method="post" action="/admin/user/{u.id}/delete"
                  onsubmit="return confirm('Nutzer &quot;{u.display_name}&quot; wirklich löschen?')">
              <button class="btn btn-danger btn-sm" type="submit">Löschen</button>
            </form>
          </td>
        </tr>"""

    users_section = f"""
    <div class="section">
      <div class="section-header">Alle Nutzer</div>
      <table>
        <tr><th>Name</th><th>Punkte</th><th>Streak</th><th>Haushalte</th>
            <th>Erstellt</th><th>Umbenennen</th><th>Löschen</th></tr>
        {user_rows}
      </table>
    </div>"""

    body = stats_html + households_section + users_section
    return _page("Dashboard", body)


@router.post("/household/{household_id}/rename")
def rename_household(
    household_id: int,
    name: str = Form(...),
    _: HTTPBasicCredentials = Depends(_verify_admin),
    db: Session = Depends(get_db),
):
    h = db.get(Household, household_id)
    if not h:
        raise HTTPException(status_code=404)
    h.name = name.strip()
    db.commit()
    return RedirectResponse("/admin", status_code=303)


@router.post("/household/{household_id}/delete")
def delete_household(
    household_id: int,
    _: HTTPBasicCredentials = Depends(_verify_admin),
    db: Session = Depends(get_db),
):
    h = db.get(Household, household_id)
    if not h:
        raise HTTPException(status_code=404)
    db.delete(h)
    db.commit()
    return RedirectResponse("/admin", status_code=303)


@router.post("/member/{member_id}/remove")
def remove_member(
    member_id: int,
    _: HTTPBasicCredentials = Depends(_verify_admin),
    db: Session = Depends(get_db),
):
    m = db.get(HouseholdMember, member_id)
    if not m:
        raise HTTPException(status_code=404)
    db.delete(m)
    db.commit()
    return RedirectResponse("/admin", status_code=303)


@router.post("/user/{user_id}/rename")
def rename_user(
    user_id: int,
    display_name: str = Form(...),
    _: HTTPBasicCredentials = Depends(_verify_admin),
    db: Session = Depends(get_db),
):
    u = db.get(User, user_id)
    if not u:
        raise HTTPException(status_code=404)
    u.display_name = display_name.strip()
    db.commit()
    return RedirectResponse("/admin", status_code=303)


@router.post("/user/{user_id}/delete")
def delete_user(
    user_id: int,
    _: HTTPBasicCredentials = Depends(_verify_admin),
    db: Session = Depends(get_db),
):
    u = db.get(User, user_id)
    if not u:
        raise HTTPException(status_code=404)
    db.delete(u)
    db.commit()
    return RedirectResponse("/admin", status_code=303)
